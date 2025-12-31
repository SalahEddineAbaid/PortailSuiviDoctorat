package ma.emsi.notificationservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Custom generator for invalid email addresses for property-based testing.
 * Generates various forms of invalid email formats to test validation logic.
 */
public class InvalidEmailGenerator extends Generator<String> {
    
    private static final String[] INVALID_EMAIL_PATTERNS = {
        // Missing @ symbol
        "userexample.com",
        "user.example.com",
        "user_example.com",
        
        // Missing domain
        "user@",
        "user@.",
        "user@domain",
        
        // Missing local part
        "@example.com",
        "@domain.com",
        
        // Multiple @ symbols
        "user@@example.com",
        "user@domain@example.com",
        
        // Invalid characters
        "user name@example.com",
        "user@exam ple.com",
        "user#name@example.com",
        "user$name@example.com",
        "user%name@example.com",
        
        // Missing TLD
        "user@domain.",
        "user@domain..",
        
        // Empty or whitespace
        "",
        " ",
        "   ",
        "\t",
        "\n",
        
        // Only special characters
        "@@@",
        "...",
        "###",
        "!!!",
        
        // Invalid format
        "user",
        "user@",
        "@domain",
        "user@domain@",
        
        // Leading/trailing dots
        ".user@example.com",
        "user.@example.com",
        "user@.example.com",
        "user@example.com.",
        
        // Consecutive dots
        "user..name@example.com",
        "user@example..com",
        
        // Invalid domain format
        "user@-example.com",
        "user@example-.com",
        "user@exam_ple.com",
        
        // Special edge cases
        "user@",
        "@",
        "@@",
        "user@@",
        "@@user",
        "user@domain@domain.com",
        "user name@domain.com",
        "user\t@domain.com",
        "user\n@domain.com",
        
        // Unicode and special characters
        "user@dömain.com",
        "üser@domain.com",
        "user@domain.cöm",
        
        // Very long invalid emails
        "a".repeat(300) + "@example.com",
        "user@" + "a".repeat(300) + ".com",
        
        // Null-like strings
        "null",
        "undefined",
        "NULL"
    };
    
    public InvalidEmailGenerator() {
        super(String.class);
    }
    
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        // Choose a random invalid pattern
        String basePattern = INVALID_EMAIL_PATTERNS[random.nextInt(INVALID_EMAIL_PATTERNS.length)];
        
        // Sometimes add random variations to make it even more invalid
        if (random.nextBoolean()) {
            return addRandomVariation(basePattern, random);
        }
        
        return basePattern;
    }
    
    /**
     * Adds random variations to make the email even more invalid.
     */
    private String addRandomVariation(String email, SourceOfRandomness random) {
        int variation = random.nextInt(5);
        
        switch (variation) {
            case 0:
                // Add random whitespace
                return email + "   ";
            case 1:
                // Add random special characters
                return email + "###";
            case 2:
                // Duplicate the string
                return email + email;
            case 3:
                // Add newline or tab
                return email + "\n";
            case 4:
                // Add unicode characters
                return email + "é€ñ";
            default:
                return email;
        }
    }
}
