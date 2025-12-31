# PDF Generation Images

This directory contains images required for PDF attestation generation.

## Required Files

### logo-etablissement.png
- **Purpose**: Institution logo displayed in the attestation header
- **Recommended size**: 200x100 pixels
- **Format**: PNG with transparent background
- **Location**: Place in this directory

### signature-chef.png
- **Purpose**: Director's signature displayed in the attestation footer
- **Recommended size**: 300x100 pixels
- **Format**: PNG with transparent background
- **Location**: Place in this directory

## Configuration

These files are referenced in `application.properties`:
```properties
pdf.logo.path=classpath:static/images/logo-etablissement.png
pdf.signature.path=classpath:static/images/signature-chef.png
```

## Note

Until actual image files are provided, the PDF generation service should handle missing images gracefully by either:
- Skipping the image section
- Using placeholder text
- Throwing a clear error message
