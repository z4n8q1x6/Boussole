package tn.esprit.boussole.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;






public class AuthService {

    private static final String SECRET;
    private static final long EXP_MS;

    private final Key key;

    static {

        Dotenv dotenv = Dotenv.load();


        SECRET = dotenv.get("JWT_SECRET");
        if (SECRET == null || SECRET.length() < 32) {
            throw new RuntimeException(
                    "JWT_SECRET non défini ou trop court ! Définissez une clé sécurisée (≥32 caractères) dans le .env"
            );
        }

        // Récupère la durée du token depuis .env ou valeur par défaut 1h
        String expStr = dotenv.get("JWT_EXP_MS", "3600000");
        EXP_MS = Long.parseLong(expStr);
    }

    public AuthService() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Génère un JWT contenant le subject (ex: username/email) et la claim "role".
     */
    public String generateToken(String subject, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXP_MS);

        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role == null ? "" : role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse et retourne les Jws<Claims>. Lance JwtException en cas d'erreur (expired, tampered, ...).
     */
    public Jws<Claims> parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    /**
     * Retourne les claims décodées (sans enveloppe Jws).
     * Lance JwtException si le token est invalide.
     */
    public Claims getClaimsFromToken(String token) throws JwtException {
        return parseToken(token).getBody();
    }

    /**
     * Récupère la claim "role" depuis le token. Retourne chaîne vide si absente.
     */
    public String getRoleFromToken(String token) throws JwtException {
        Claims claims = getClaimsFromToken(token);
        Object role = claims.get("role");
        return role == null ? "" : String.valueOf(role);
    }

    /**
     * Vérifie si le token est valide et non expiré.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date exp = claims.getExpiration();
            return exp == null || exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
