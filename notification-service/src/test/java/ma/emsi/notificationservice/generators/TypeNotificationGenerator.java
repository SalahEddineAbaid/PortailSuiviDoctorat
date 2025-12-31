package ma.emsi.notificationservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import ma.emsi.notificationservice.enums.TypeNotification;

/**
 * Custom generator for TypeNotification enum for property-based testing.
 * Generates random notification types covering all possible values.
 */
public class TypeNotificationGenerator extends Generator<TypeNotification> {
    
    public TypeNotificationGenerator() {
        super(TypeNotification.class);
    }
    
    @Override
    public TypeNotification generate(SourceOfRandomness random, GenerationStatus status) {
        TypeNotification[] types = TypeNotification.values();
        return types[random.nextInt(types.length)];
    }
}
