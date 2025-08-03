# Iran Market IAP Flutter Plugin

A Flutter plugin for in-app purchases supporting Myket and CafeBazaar markets using Platform Channels.

## Features

- ✅ Support for both CafeBazaar and Myket markets
- ✅ Runtime market switching via environment variables
- ✅ Platform channel communication (no official SDK dependencies)
- ✅ Custom AIDL interface for billing operations
- ✅ Complete purchase lifecycle management
- ✅ Product details and purchase history
- ✅ Purchase consumption and acknowledgment
- ✅ **RSA signature verification for security**

## Installation

Add the dependency to your `pubspec.yaml`:

```yaml
dependencies:
  iran_market_iap: ^0.0.1
  flutter_dotenv: ^5.1.0
```

## Setup

### 1. Environment Configuration

Create a `.env` file in your project root:

```env
MARKET_TYPE=cafebazaar
BAZAAR_RSA_PUBLIC_KEY=your_bazaar_public_key_here
MYKET_RSA_PUBLIC_KEY=your_myket_public_key_here
```

Supported market types:
- `cafebazaar` - CafeBazaar market
- `myket` - Myket market

### 2. RSA Key Configuration

#### Getting RSA Public Keys

**For CafeBazaar:**
1. Go to your CafeBazaar Developer Console
2. Navigate to your app's settings
3. Find the "In-App Billing" section
4. Copy the RSA public key provided by CafeBazaar

**For Myket:**
1. Go to your Myket Developer Console
2. Navigate to your app's settings
3. Find the "In-App Billing" section
4. Copy the RSA public key provided by Myket

#### Setting RSA Keys

```dart
// Set RSA key configuration
final rsaConfig = RSAKeyConfig(
  publicKey: 'your_rsa_public_key_here',
);
MarketIAP.setRSAKeyConfig(rsaConfig);
```

### 3. Android Permissions

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="com.farsitel.bazaar.permission.PAYMENT" />
<uses-permission android:name="ir.mservices.market.permission.PAYMENT" />
<uses-permission android:name="android.permission.INTERNET" />
```

### 4. Assets Configuration

Add the `.env` file to your assets in `pubspec.yaml`:

```yaml
flutter:
  assets:
    - .env
```

## Usage

### Basic Setup

```dart
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:iran_market_iap/market_iap.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env');
  runApp(MyApp());
}
```

### Initialize the Plugin with RSA Keys

```dart
// Initialize the plugin
final initialized = await MarketIAP.initialize();

if (initialized) {
  // Set RSA key configuration
  final rsaConfig = RSAKeyConfig(
    publicKey: MarketIAP.getRSAKeyForMarket(),
  );
  MarketIAP.setRSAKeyConfig(rsaConfig);
  
  print('Market IAP initialized for ${MarketIAP.currentMarketType}');
} else {
  print('Failed to initialize Market IAP');
}
```

### Check Billing Support

```dart
// Check if billing is supported
final isSupported = await MarketIAP.isBillingSupported();
print('Billing supported: $isSupported');
```

### Get Product Details

```dart
// Get product details
final productIds = ['premium_feature', 'remove_ads'];
final products = await MarketIAP.getProducts(productIds);

for (final product in products) {
  print('Product: ${product.title} - ${product.price}');
}
```

### Purchase a Product

```dart
// Purchase a product
final purchase = await MarketIAP.purchase(
  'premium_feature',
  developerPayload: 'user_123',
);

if (purchase != null) {
  print('Purchase successful: ${purchase.productId}');
  print('Order ID: ${purchase.orderId}');
  print('Purchase Token: ${purchase.purchaseToken}');
  
  // Verify purchase signature
  final isValid = await MarketIAP.verifyPurchaseSignature(purchase);
  print('Purchase signature valid: $isValid');
} else {
  print('Purchase failed or cancelled');
}
```

### Get Purchases (with signature verification)

```dart
// Get all purchases (signatures are automatically verified if RSA key is configured)
final purchases = await MarketIAP.getPurchases();

for (final purchase in purchases) {
  print('Purchased: ${purchase.productId}');
  print('Order: ${purchase.orderId}');
  print('Signature: ${purchase.signature != null ? 'Verified' : 'Not verified'}');
}
```

### Consume a Purchase

```dart
// Consume a purchase
final success = await MarketIAP.consume(purchaseToken);

if (success) {
  print('Purchase consumed successfully');
} else {
  print('Failed to consume purchase');
}
```

### Check Purchase Status

```dart
// Check if a product is purchased
final isPurchased = await MarketIAP.isPurchased('premium_feature');
print('Premium feature purchased: $isPurchased');
```

### Get Purchase History

```dart
// Get purchase history (signatures are automatically verified)
final history = await MarketIAP.getPurchaseHistory();

for (final purchase in history) {
  print('History: ${purchase.productId} - ${purchase.purchaseTime}');
  print('Signature verified: ${purchase.signature != null}');
}
```

### Acknowledge Purchase

```dart
// Acknowledge a purchase
final success = await MarketIAP.acknowledgePurchase(
  purchaseToken,
  developerPayload: 'acknowledgment',
);

if (success) {
  print('Purchase acknowledged successfully');
} else {
  print('Failed to acknowledge purchase');
}
```

### Verify Purchase Signature Manually

```dart
// Verify a purchase signature manually
final isValid = await MarketIAP.verifyPurchaseSignature(purchaseData);

if (isValid) {
  print('Purchase signature is valid');
} else {
  print('Purchase signature is invalid');
}
```

## RSA Key Security

### Why RSA Keys are Important

RSA signature verification is crucial for security because:

1. **Prevents Fraud**: Ensures purchases are actually made through the legitimate market
2. **Data Integrity**: Verifies that purchase data hasn't been tampered with
3. **Revenue Protection**: Prevents fake purchase claims

### How It Works

1. **Market Signs Purchase**: When a purchase is made, the market (CafeBazaar/Myket) signs the purchase data with their private key
2. **App Verifies Signature**: Your app uses the market's public key to verify the signature
3. **Secure Processing**: Only verified purchases are processed

### Best Practices

1. **Always Verify Signatures**: Never process purchases without signature verification
2. **Store Keys Securely**: Keep RSA public keys in environment variables, not in code
3. **Use Different Keys**: Each market has its own RSA key
4. **Test Verification**: Always test signature verification in development

## Architecture

### Dart Layer
- `MarketIAP` class provides the main interface
- Uses `flutter_dotenv` to load market type and RSA keys from environment
- Communicates with native code via `MethodChannel`
- Includes `RSAKeyConfig` for key management

### Android Layer
- Custom AIDL interface (`IInAppBillingService.aidl`)
- Separate implementations for CafeBazaar (`BazaarBilling.kt`) and Myket (`MyketBilling.kt`)
- Platform channel handler (`MarketIapPlugin.kt`) routes calls to appropriate implementation
- **RSA signature verification using SHA256withRSA algorithm**

### Key Components

1. **AIDL Interface**: Defines the contract for billing operations
2. **BazaarBilling**: Handles CafeBazaar market billing
3. **MyketBilling**: Handles Myket market billing
4. **MarketIapPlugin**: Main plugin class that handles platform communication
5. **RSA Verification**: Secure signature verification for all purchases

## Example App

The `/example` folder contains a complete Flutter app demonstrating all plugin features:

- Initialize the plugin with RSA keys
- Check billing support
- Get product details
- Purchase products with signature verification
- Manage purchases with automatic signature verification
- Handle purchase consumption
- Manual signature verification

To run the example:

```bash
cd example
flutter pub get
flutter run
```

## Market-Specific Notes

### CafeBazaar
- Package: `com.farsitel.bazaar`
- Service: `com.farsitel.bazaar.service.InAppBillingService.BIND`
- Permission: `com.farsitel.bazaar.permission.PAYMENT`
- RSA Key: Get from CafeBazaar Developer Console

### Myket
- Package: `ir.mservices.market`
- Service: `ir.mservices.market.service.InAppBillingService.BIND`
- Permission: `ir.mservices.market.permission.PAYMENT`
- RSA Key: Get from Myket Developer Console

## Error Handling

The plugin includes comprehensive error handling:

- Network connectivity issues
- Market service unavailability
- Invalid product IDs
- Purchase failures
- Service binding errors
- **RSA signature verification failures**

All methods return appropriate error states and log detailed error messages for debugging.

## Security Considerations

1. **Never Store Private Keys**: Only public keys should be in your app
2. **Verify All Purchases**: Always verify purchase signatures before processing
3. **Use HTTPS**: Ensure all network communications are secure
4. **Regular Key Updates**: Keep RSA keys updated as provided by markets
5. **Test Thoroughly**: Test signature verification in all scenarios

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
