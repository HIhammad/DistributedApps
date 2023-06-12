package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.JsonFactory;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.objectweb.asm.TypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

//class ResponsePublicKey
//{
//    @SerializedName("e")
//    private String e;
//    @SerializedName("n")
//    private String n;
//    @SerializedName("kty")
//    private String kty;
//    @SerializedName("kid")
//    private String kid;
//    @SerializedName("use")
//    private String use;
//    @SerializedName("alg")
//    private String alg;
//
//    public String getE() {
//        return e;
//    }
//
//    public String getN() {
//        return n;
//    }
//
//    public String getKty() {
//        return kty;
//    }
//
//    public String getKid() {
//        return kid;
//    }
//
//    public String getUse() {
//        return use;
//    }
//
//    public String getAlg() {
//        return alg;
//    }
//}
//
//class ResponseArray
//{
//    @SerializedName("keys")
//    private ArrayList<ResponsePublicKey> keys;
//    public ArrayList<ResponsePublicKey> getKeys() {
//        return keys;
//    }
//}

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private Map<String, RSAPublicKey> PUBLIC_KEYS;

    protected void updatePublicKeys()
    {
//        String publicKeyUrl = "https://www.googleapis.com/oauth2/v3/certs";
        String publicKeyUrl = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

        PUBLIC_KEYS = new HashMap<>();

        HttpUriRequest uriRequest = new HttpGet(publicKeyUrl);

        HttpResponse response;
        try
        {
            CloseableHttpClient client = HttpClientBuilder.create().build();

            response = client.execute(uriRequest);
            org.apache.http.HttpEntity entity = response.getEntity();

            if (entity != null)
            {
                String content = EntityUtils.toString(entity);

                try
                {
                    content = content.replace("{", "").replace("}", "").replace("\"", "").replace("\n", "");

                    String[] keyEntries = content.split(",");

                    for (String keyEntry : keyEntries)
                    {

                        String[] keyParts = keyEntry.split(":");

                        String keyId = keyParts[0].trim();      // TODO can be removed?
                        String encodedKey = keyParts[1].trim();

                        encodedKey = encodedKey.replace("-----BEGIN CERTIFICATE-----", "")
                                .replace("-----END CERTIFICATE-----", "")
                                .replace("\\n", "")
                                .trim();

                        byte[] certBytes = Base64.getDecoder().decode(encodedKey);
                        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
                        PublicKey publicKey = cert.getPublicKey();

                        PUBLIC_KEYS.put(keyId, (RSAPublicKey) publicKey);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

//                Gson gson = new Gson();
//                ResponseArray responses = gson.fromJson(content, ResponseArray.class);
//
//                for (ResponsePublicKey publicKey : responses.getKeys())
//                {
//                    byte[] eBytes = Base64.getUrlDecoder().decode(publicKey.getE().getBytes(StandardCharsets.UTF_8));
//                    byte[] nBytes = Base64.getUrlDecoder().decode(publicKey.getN().getBytes(StandardCharsets.UTF_8));
//
//                    java.math.BigInteger exponent = new java.math.BigInteger(1, eBytes);
//                    java.math.BigInteger modulus = new java.math.BigInteger(1, nBytes);
//
//                    try {
//                        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
//
//                        KeyFactory keyFactory = KeyFactory.getInstance(publicKey.getKty());
//                        PublicKey GeneratedPublicKey = keyFactory.generatePublic(keySpec);
//
//                        PUBLIC_KEYS.put(publicKey.getKid(), (RSAPublicKey) GeneratedPublicKey);
//
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
        } catch (Exception e)
        {
            return; // TODO
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        updatePublicKeys(); // TODO at more efficient location

        var header = request.getHeader("Authorization");
        header = header.replace("Bearer ", "");

        try
        {
            var kid = JWT.decode(header).getKeyId();
            //kid = "6083dd5981673f661fde9dae646b6f0380a0145c";
            if (kid != null) {      // TODO more secure way?
                System.out.println(kid);
                var pubKey = PUBLIC_KEYS.get(kid);
                Algorithm algorithm = Algorithm.RSA256(pubKey, null);
                DecodedJWT jwt = JWT.require(algorithm)
                        .withIssuer("https://securetoken.google.com/" + "distributed-apps")
                        .build()
                        .verify(header);
                System.out.println("It has been verified");
            }
        } catch (JWTVerificationException e)
        {
            // unauthorized access
            return; // TODO check if allowed here? Might solve problem below
        }

        // TODO likely has to be moved up
        var role = JWT.decode(header).getClaim("role");
        var name = JWT.decode(header).getClaim("email");
        var user = new User(name.toString().replace("\"", ""), role.toString().replace("\"", ""));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new FirebaseAuthentication(user));

        filterChain.doFilter(request, response);
    }



    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api");
    }

    private static class FirebaseAuthentication implements Authentication {
        private final User user;

        FirebaseAuthentication(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            if (user.isManager()) {
                return List.of(new SimpleGrantedAuthority("manager"));
            } else {
                return new ArrayList<>();
            }
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public User getPrincipal() {
            return this.user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {

        }

        @Override
        public String getName() {
            return null;
        }
    }
}

