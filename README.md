# Iran Market IAP

A Flutter plugin for in-app purchases supporting **Myket** and **CafeBazaar** markets using Platform Channels.

## Features

- ✅ **Dual Market Support**: CafeBazaar and Myket
- ✅ **Environment Configuration**: Configure via `.env` file
- ✅ **App Detection**: Automatically detects installed market apps
- ✅ **RSA Key Support**: Signature verification for both markets
- ✅ **Error Handling**: Comprehensive error handling and logging
- ✅ **Market Switching**: Dynamic market switching based on configuration

## Installation

Add this to your `pubspec.yaml`:

```yaml
dependencies:
  iran_market_iap: ^0.0.17
```

## Configuration

Create a `.env` file in your assets folder:

```env
# Market Configuration
MARKET_TYPE=cafebazaar

# RSA Keys (replace with your actual keys)
CAFEBAZAAR_RSA_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
MYKET_RSA_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...

# Debug Settings
DEBUG_LOGGING=true
```

## Usage

### Basic Initialization

```dart
import 'package:iran_market_iap/iran_market_iap.dart';

void main() async {
  // Initialize with market from .env file
  final result = await MarketIAP.init();
  print('Initialization result: $result');
}
```

### Advanced Initialization

```dart
// Initialize with specific market and RSA key
final result = await MarketIAP.init(
  market: 'myket',
  rsaKey: 'your_rsa_key_here',
  enableDebugLogging: true,
);
```

### Connect to Billing Service

```dart
// Connect to billing service
final connectResult = await MarketIAP.connect();
if (connectResult['success'] == true) {
  print('Connected successfully to ${connectResult['market']}');
}
```

### Purchase Products

```dart
// Purchase a product
final purchaseResult = await MarketIAP.purchase(
  'product_id',
  developerPayload: 'optional_payload',
);

if (purchaseResult['success'] == true) {
  print('Purchase successful!');
} else {
  print('Purchase failed: ${purchaseResult['error']}');
}
```

### Consume Purchases

```dart
// Consume a purchase
final consumeResult = await MarketIAP.consume('purchase_token');
if (consumeResult['success'] == true) {
  print('Purchase consumed successfully');
}
```

### Get Purchases

```dart
// Get all purchases
final purchasesResult = await MarketIAP.getPurchases();
if (purchasesResult['success'] == true) {
  final purchases = purchasesResult['purchases'] as List;
  print('Found ${purchases.length} purchases');
}
```

### Get SKU Details

```dart
// Get SKU details
final skuResult = await MarketIAP.getSkuDetails(['sku1', 'sku2']);
if (skuResult['success'] == true) {
  final skuDetails = skuResult['skuDetails'] as List;
  print('Found ${skuDetails.length} SKU details');
}
```

### Check Billing Support

```dart
// Check if billing is supported
final isSupported = await MarketIAP.isBillingSupported();
print('Billing supported: $isSupported');
```

### Disconnect

```dart
// Disconnect from billing service
await MarketIAP.disconnect();
```

## API Reference

### MarketIAP.init()

Initialize the plugin with market configuration.

**Parameters:**
- `market` (String, optional): Market type ('cafebazaar' or 'myket')
- `rsaKey` (String, optional): RSA public key for signature verification
- `enableDebugLogging` (bool, optional): Enable debug logging

**Returns:** Map with initialization result

### MarketIAP.connect()

Connect to the billing service.

**Returns:** Map with connection result

### MarketIAP.purchase(productId, developerPayload)

Purchase a product.

**Parameters:**
- `productId` (String): Product ID to purchase
- `developerPayload` (String, optional): Developer payload

**Returns:** Map with purchase result

### MarketIAP.consume(purchaseToken)

Consume a purchase.

**Parameters:**
- `purchaseToken` (String): Purchase token to consume

**Returns:** Map with consume result

### MarketIAP.getPurchases()

Get all purchased products.

**Returns:** Map with purchases list

### MarketIAP.getSkuDetails(skuIds)

Get SKU details for products.

**Parameters:**
- `skuIds` (List<String>): List of SKU IDs

**Returns:** Map with SKU details

### MarketIAP.isBillingSupported()

Check if billing is supported.

**Returns:** bool indicating billing support

### MarketIAP.disconnect()

Disconnect from billing service.

**Returns:** Map with disconnect result

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MARKET_TYPE` | Market to use ('cafebazaar' or 'myket') | 'cafebazaar' |
| `CAFEBAZAAR_RSA_KEY` | RSA public key for CafeBazaar | null |
| `MYKET_RSA_KEY` | RSA public key for Myket | null |
| `DEBUG_LOGGING` | Enable debug logging | true |

## Error Handling

The plugin provides comprehensive error handling:

```dart
try {
  final result = await MarketIAP.purchase('product_id');
  if (result['success'] == true) {
    // Handle success
  } else {
    // Handle error
    print('Error: ${result['error']}');
  }
} catch (e) {
  print('Exception: $e');
}
```

## Logging

Enable debug logging to see detailed information:

```dart
await MarketIAP.init(enableDebugLogging: true);
```

Logs will show:
- Market detection
- App installation status
- Service connection attempts
- Purchase flow details
- Error messages

## Platform Support

- ✅ Android (API 21+)
- ❌ iOS (Not supported - Iran markets are Android-only)

## Dependencies

- Flutter 3.0+
- Android API 21+
- Kotlin 1.9.20+

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
