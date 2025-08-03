# Changelog

All notable changes to the `iran_market_iap` package will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - 2024-12-19

### Added
- **Initial release** of Iran Market IAP Flutter Plugin
- **Dual market support** for CafeBazaar and Myket in-app purchases
- **Platform channel communication** without official SDK dependencies
- **Custom AIDL interface** (`IInAppBillingService.aidl`) for billing operations
- **Environment-based configuration** using `.env` files for market type selection
- **Runtime market switching** via `MARKET_TYPE` environment variable
- **Complete purchase lifecycle management**:
  - Product details retrieval
  - Purchase initiation and completion
  - Purchase consumption and acknowledgment
  - Purchase history and status checking
- **RSA signature verification** for secure purchase validation
- **Android-only platform support** with proper plugin registration
- **Comprehensive error handling** for all billing operations
- **Type-safe data models**:
  - `PurchaseData` with signature support
  - `ProductData` for product information
  - `RSAKeyConfig` for key management
  - `MarketType` enum for market selection

### Features
- **Market IAP Class**: Main interface for all billing operations
- **BazaarBilling.kt**: CafeBazaar market implementation
- **MyketBilling.kt**: Myket market implementation
- **MarketIapPlugin.kt**: Platform channel handler with RSA verification
- **Automatic signature verification** for all purchases when RSA keys are configured
- **Manual signature verification** capability
- **Environment-based RSA key configuration** for both markets
- **Comprehensive example app** demonstrating all features
- **Complete test coverage** with 28 passing tests

### Security
- **RSA signature verification** using SHA256withRSA algorithm
- **Automatic purchase validation** to prevent fraud
- **Secure key management** through environment variables
- **Data integrity verification** for all purchase data

### Documentation
- **Comprehensive README** with setup and usage instructions
- **Security best practices** documentation
- **Market-specific configuration** guides
- **Complete API documentation** with examples
- **Architecture overview** and component descriptions

### Technical Details
- **Minimum SDK**: Android API 21+
- **Target SDK**: Android API 34
- **Kotlin version**: 1.9.0
- **Flutter SDK**: ^3.8.1
- **Dependencies**: flutter_dotenv ^5.1.0

### Example Usage
```dart
// Initialize with RSA keys
await MarketIAP.initialize();
final rsaConfig = RSAKeyConfig(publicKey: 'your_rsa_key');
MarketIAP.setRSAKeyConfig(rsaConfig);

// Purchase with verification
final purchase = await MarketIAP.purchase('product_id');
final isValid = await MarketIAP.verifyPurchaseSignature(purchase);
```

### Breaking Changes
- None (initial release)

### Deprecations
- None (initial release)

### Known Issues
- None reported

### Migration Guide
- Not applicable (initial release)
