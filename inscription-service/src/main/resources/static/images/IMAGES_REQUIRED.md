# Required Images for PDF Generation

This directory should contain the following images for attestation PDF generation:

## 1. logo-etablissement.png
- **Purpose**: Institution logo displayed in the header of attestation documents
- **Recommended size**: 100x100 pixels
- **Format**: PNG with transparent background
- **Location**: `src/main/resources/static/images/logo-etablissement.png`

## 2. signature-chef.png
- **Purpose**: Director's signature displayed at the bottom of attestation documents
- **Recommended size**: 150x75 pixels
- **Format**: PNG with transparent background
- **Location**: `src/main/resources/static/images/signature-chef.png`

## Notes
- If these images are not present, the PDF generator will use text placeholders
- The application will log warnings if images cannot be loaded
- Images should be optimized for PDF embedding to keep file sizes reasonable
