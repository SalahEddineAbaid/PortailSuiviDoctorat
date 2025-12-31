# Setup Notes for Inscription Service Finalisation

## Task 1: Dependencies and Configuration - COMPLETED

### Dependencies Added

#### 1. ZXing Library for QR Code Generation
Added to `pom.xml`:
```xml
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>

<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

#### 2. JUnit QuickCheck for Property-Based Testing
Added to `pom.xml`:
```xml
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-core</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-generators</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```

### Configuration Properties Added

Added to `application.properties`:

#### Document Validation Configuration
```properties
upload.allowed-types=application/pdf,image/jpeg,image/png
upload.max-size=10485760
upload.virus-scan.enabled=false
upload.virus-scan.clamav.host=localhost
upload.virus-scan.clamav.port=3310
```

#### PDF Generation Configuration
```properties
pdf.logo.path=classpath:static/images/logo-etablissement.png
pdf.signature.path=classpath:static/images/signature-chef.png
pdf.qrcode.base-url=https://portail.emsi.ma/verify/attestation/
pdf.attestation.output-dir=./uploads/attestations
```

#### Alert Duration Thresholds
```properties
alertes.duree.seuil-3-ans=2.5
alertes.duree.seuil-6-ans=5.5
alertes.duree.limite-max=6.0
```

### Resource Files Directory Structure

Created directory structure:
```
inscription-service/src/main/resources/static/images/
├── .gitkeep
└── README.md
```

**Required image files** (to be added by the team):
- `logo-etablissement.png` - Institution logo (200x100px recommended)
- `signature-chef.png` - Director signature (300x100px recommended)

### Fixed Issues

1. Removed duplicate dependencies:
   - `spring-boot-devtools` (was declared twice)
   - `spring-boot-starter-test` (was declared twice)

### Environment Requirements

**Important**: This project requires Java 17 or higher.

Current environment status:
- Java 21 is installed on the system
- JAVA_HOME is currently pointing to Java 8 (`C:\Program Files\Java\jdk1.8.0_77`)

**Action Required**: Update JAVA_HOME environment variable to point to Java 17+ installation before building:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

Or permanently set it in System Environment Variables.

### Validation

To validate the setup after fixing JAVA_HOME:
```bash
cd inscription-service
./mvnw.cmd clean compile -DskipTests
```

### Next Steps

1. Update JAVA_HOME environment variable
2. Add actual logo and signature image files to `src/main/resources/static/images/`
3. Proceed with Task 2: Implement document validation service
