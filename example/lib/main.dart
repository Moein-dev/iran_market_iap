import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:iran_market_iap/market_iap.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env');
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Market IAP Example',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Market IAP Example'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  bool _isInitialized = false;
  bool _isBillingSupported = false;
  List<ProductData> _products = [];
  List<PurchaseData> _purchases = [];
  String _statusMessage = '';

  @override
  void initState() {
    super.initState();
    _initializeMarketIAP();
  }

  /// Initialize the Market IAP plugin
  Future<void> _initializeMarketIAP() async {
    try {
      setState(() {
        _statusMessage = 'Initializing Market IAP...';
      });

      final initialized = await MarketIAP.initialize();
      
      if (initialized) {
        final marketType = MarketIAP.currentMarketType;
        setState(() {
          _isInitialized = true;
          _statusMessage = 'Market IAP initialized successfully for ${marketType?.name}';
        });

        // Set RSA key configuration
        _setupRSAKeys();

        // Check if billing is supported
        await _checkBillingSupport();
      } else {
        setState(() {
          _statusMessage = 'Failed to initialize Market IAP';
        });
      }
    } catch (e) {
      setState(() {
        _statusMessage = 'Error initializing Market IAP: $e';
      });
    }
  }

  /// Setup RSA keys for purchase verification
  void _setupRSAKeys() {
    try {
      final marketType = MarketIAP.currentMarketType;
      String publicKey = '';

      // Get RSA key based on market type
      switch (marketType) {
        case MarketType.cafebazaar:
          publicKey = dotenv.env['BAZAAR_RSA_PUBLIC_KEY'] ?? '';
          break;
        case MarketType.myket:
          publicKey = dotenv.env['MYKET_RSA_PUBLIC_KEY'] ?? '';
          break;
        default:
          publicKey = '';
      }

      if (publicKey.isNotEmpty) {
        final rsaConfig = RSAKeyConfig(publicKey: publicKey);
        MarketIAP.setRSAKeyConfig(rsaConfig);
        setState(() {
          _statusMessage = 'RSA key configured for ${marketType?.name}';
        });
      } else {
        setState(() {
          _statusMessage = 'Warning: No RSA key configured for ${marketType?.name}';
        });
      }
    } catch (e) {
      setState(() {
        _statusMessage = 'Error setting up RSA keys: $e';
      });
    }
  }

  /// Check if billing is supported
  Future<void> _checkBillingSupport() async {
    try {
      final isSupported = await MarketIAP.isBillingSupported();
      setState(() {
        _isBillingSupported = isSupported;
        _statusMessage = 'Billing supported: $isSupported';
      });
    } catch (e) {
      setState(() {
        _statusMessage = 'Error checking billing support: $e';
      });
    }
  }

  /// Get product details
  Future<void> _getProducts() async {
    try {
      setState(() {
        _statusMessage = 'Getting products...';
      });

      // Example product IDs - replace with your actual product IDs
      final productIds = ['premium_feature', 'remove_ads', 'unlock_all'];
      final products = await MarketIAP.getProducts(productIds);
      
      setState(() {
        _products = products;
        _statusMessage = 'Found ${products.length} products';
      });
    } catch (e) {
      setState(() {
        _statusMessage = 'Error getting products: $e';
      });
    }
  }

  /// Purchase a product
  Future<void> _purchaseProduct(String productId) async {
    try {
      setState(() {
        _statusMessage = 'Purchasing $productId...';
      });

      final purchase = await MarketIAP.purchase(productId, developerPayload: 'example_payload');
      
      if (purchase != null) {
        setState(() {
          _statusMessage = 'Purchase successful: ${purchase.productId}';
        });
        
        // Verify purchase signature
        await _verifyPurchaseSignature(purchase);
        
        // Refresh purchases list
        await _getPurchases();
      } else {
        setState(() {
          _statusMessage = 'Purchase failed or cancelled';
        });
      }
    } catch (e) {
      setState(() {
        _statusMessage = 'Error purchasing product: $e';
      });
    }
  }

  /// Verify purchase signature
  Future<void> _verifyPurchaseSignature(PurchaseData purchase) async {
    try {
      final isValid = await MarketIAP.verifyPurchaseSignature(purchase);
      setState(() {
        _statusMessage = 'Purchase signature verification: ${isValid ? 'Valid' : 'Invalid'}';
      });
    } catch (e) {
      setState(() {
        _statusMessage = 'Error verifying purchase signature: $e';
      });
    }
  }

  /// Get all purchases
  Future<void> _getPurchases() async {
    try {
      setState(() {
        _statusMessage = 'Getting purchases...';
      });

      final purchases = await MarketIAP.getPurchases();
      
      setState(() {
        _purchases = purchases;
        _statusMessage = 'Found ${purchases.length} purchases (signatures verified)';
      });
    } catch (e) {
      setState(() {
        _statusMessage = 'Error getting purchases: $e';
      });
    }
  }

  /// Consume a purchase
  Future<void> _consumePurchase(String purchaseToken) async {
    try {
      setState(() {
        _statusMessage = 'Consuming purchase...';
      });

      final success = await MarketIAP.consume(purchaseToken);
      
      if (success) {
        setState(() {
          _statusMessage = 'Purchase consumed successfully';
        });
        
        // Refresh purchases list
        await _getPurchases();
      } else {
        setState(() {
          _statusMessage = 'Failed to consume purchase';
        });
      }
    } catch (e) {
      setState(() {
        _statusMessage = 'Error consuming purchase: $e';
      });
    }
  }

  /// Check if a product is purchased
  Future<void> _checkIfPurchased(String productId) async {
    try {
      final isPurchased = await MarketIAP.isPurchased(productId);
      setState(() {
        _statusMessage = '$productId is purchased: $isPurchased';
      });
    } catch (e) {
      setState(() {
        _statusMessage = 'Error checking purchase status: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            // Status section
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Status',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Text('Initialized: $_isInitialized'),
                    Text('Billing Supported: $_isBillingSupported'),
                    Text('Current Market: ${MarketIAP.currentMarketType?.name ?? 'Unknown'}'),
                    Text('RSA Key Configured: ${MarketIAP.currentRSAKeyConfig != null}'),
                    const SizedBox(height: 8),
                    Text(
                      _statusMessage,
                      style: const TextStyle(fontStyle: FontStyle.italic),
                    ),
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Action buttons
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Actions',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        ElevatedButton(
                          onPressed: _isInitialized ? _getProducts : null,
                          child: const Text('Get Products'),
                        ),
                        ElevatedButton(
                          onPressed: _isInitialized ? _getPurchases : null,
                          child: const Text('Get Purchases'),
                        ),
                        ElevatedButton(
                          onPressed: _isInitialized ? () => _purchaseProduct('premium_feature') : null,
                          child: const Text('Purchase Premium'),
                        ),
                        ElevatedButton(
                          onPressed: _isInitialized ? () => _checkIfPurchased('premium_feature') : null,
                          child: const Text('Check Premium'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Products section
            if (_products.isNotEmpty) ...[
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Products (${_products.length})',
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                      const SizedBox(height: 8),
                      ...(_products.map((product) => ListTile(
                        title: Text(product.title),
                        subtitle: Text('${product.price} - ${product.description}'),
                        trailing: ElevatedButton(
                          onPressed: () => _purchaseProduct(product.productId),
                          child: const Text('Buy'),
                        ),
                      ))),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
            ],
            
            // Purchases section
            if (_purchases.isNotEmpty) ...[
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Purchases (${_purchases.length})',
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                      const SizedBox(height: 8),
                      ...(_purchases.map((purchase) => ListTile(
                        title: Text(purchase.productId),
                        subtitle: Text('Order: ${purchase.orderId}'),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            if (purchase.signature != null)
                              Icon(
                                Icons.verified,
                                color: Colors.green,
                                size: 16,
                              ),
                            const SizedBox(width: 8),
                            ElevatedButton(
                              onPressed: () => _consumePurchase(purchase.purchaseToken),
                              child: const Text('Consume'),
                            ),
                          ],
                        ),
                      ))),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
} 