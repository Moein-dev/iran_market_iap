import 'package:flutter/material.dart';
import 'package:iran_market_iap/iran_market_iap.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Iran Market IAP Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MarketIAPDemo(),
    );
  }
}

class MarketIAPDemo extends StatefulWidget {
  const MarketIAPDemo({super.key});

  @override
  State<MarketIAPDemo> createState() => _MarketIAPDemoState();
}

class _MarketIAPDemoState extends State<MarketIAPDemo> {
  String _status = 'Initializing...';
  String _currentMarket = 'Unknown';
  bool _isInitialized = false;
  bool _isBillingSupported = false;
  List<PurchaseData> _purchases = [];
  List<ProductData> _products = [];

  @override
  void initState() {
    super.initState();
    _initializePlugin();
  }

  Future<void> _initializePlugin() async {
    try {
      setState(() {
        _status = 'Initializing plugin...';
      });

      // Initialize the plugin
      final initialized = await MarketIAP.initialize();
      
      if (initialized) {
        setState(() {
          _isInitialized = true;
          _currentMarket = MarketIAP.currentMarketType?.name ?? 'Unknown';
          _status = 'Initialized successfully for $_currentMarket';
        });

        // Check billing support
        await _checkBillingSupport();
      } else {
        setState(() {
          _status = 'Initialization failed';
        });
      }
    } catch (e) {
      setState(() {
        _status = 'Error during initialization: $e';
      });
    }
  }

  Future<void> _checkBillingSupport() async {
    try {
      final isSupported = await MarketIAP.isBillingSupported();
      setState(() {
        _isBillingSupported = isSupported;
        _status = 'Billing supported: $isSupported';
      });
    } catch (e) {
      setState(() {
        _status = 'Error checking billing support: $e';
      });
    }
  }

  Future<void> _purchaseProduct() async {
    try {
      setState(() {
        _status = 'Starting purchase...';
      });

      final purchase = await MarketIAP.purchase(
        'test_product_1',
        developerPayload: 'test_payload_${DateTime.now().millisecondsSinceEpoch}',
      );

      if (purchase != null) {
        setState(() {
          _status = 'Purchase successful! Product: ${purchase.productId}';
        });
        
        // Refresh purchases list
        await _getPurchases();
      } else {
        setState(() {
          _status = 'Purchase failed or cancelled';
        });
      }
    } catch (e) {
      setState(() {
        _status = 'Error during purchase: $e';
      });
    }
  }

  Future<void> _getPurchases() async {
    try {
      setState(() {
        _status = 'Getting purchases...';
      });

      final purchases = await MarketIAP.getPurchases();
      
      setState(() {
        _purchases = purchases;
        _status = 'Found ${purchases.length} purchases';
      });
    } catch (e) {
      setState(() {
        _status = 'Error getting purchases: $e';
      });
    }
  }

  Future<void> _getProducts() async {
    try {
      setState(() {
        _status = 'Getting products...';
      });

      final products = await MarketIAP.getProducts(['test_product_1', 'test_product_2']);
      
      setState(() {
        _products = products;
        _status = 'Found ${products.length} products';
      });
    } catch (e) {
      setState(() {
        _status = 'Error getting products: $e';
      });
    }
  }

  Future<void> _consumePurchase() async {
    if (_purchases.isEmpty) {
      setState(() {
        _status = 'No purchases available to consume';
      });
      return;
    }

    try {
      final purchaseToken = _purchases.first.purchaseToken;
      
      setState(() {
        _status = 'Consuming purchase...';
      });

      final success = await MarketIAP.consume(purchaseToken);
      
      if (success) {
        setState(() {
          _status = 'Purchase consumed successfully';
        });
        
        // Refresh purchases list
        await _getPurchases();
      } else {
        setState(() {
          _status = 'Failed to consume purchase';
        });
      }
    } catch (e) {
      setState(() {
        _status = 'Error consuming purchase: $e';
      });
    }
  }

  Future<void> _checkIfPurchased() async {
    try {
      setState(() {
        _status = 'Checking purchase status...';
      });

      final isPurchased = await MarketIAP.isPurchased('test_product_1');
      
      setState(() {
        _status = 'test_product_1 is purchased: $isPurchased';
      });
    } catch (e) {
      setState(() {
        _status = 'Error checking purchase status: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('Iran Market IAP Demo'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Status Section
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
                    Text('Current Market: $_currentMarket'),
                    Text('Initialized: $_isInitialized'),
                    Text('Billing Supported: $_isBillingSupported'),
                    const SizedBox(height: 8),
                    Text(
                      _status,
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Action Buttons
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
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        ElevatedButton(
                          onPressed: _isInitialized ? _purchaseProduct : null,
                          child: const Text('Purchase Product'),
                        ),
                        ElevatedButton(
                          onPressed: _isInitialized ? _getProducts : null,
                          child: const Text('Get Products'),
                        ),
                        ElevatedButton(
                          onPressed: _isInitialized ? _getPurchases : null,
                          child: const Text('Get Purchases'),
                        ),
                        ElevatedButton(
                          onPressed: _isInitialized ? _consumePurchase : null,
                          child: const Text('Consume Purchase'),
                        ),
                        ElevatedButton(
                          onPressed: _isInitialized ? _checkIfPurchased : null,
                          child: const Text('Check Purchase'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Products Section
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
                        trailing: Text(product.productId),
                      ))),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Purchases Section
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
                        trailing: Text(purchase.purchaseToken.substring(0, 10) + '...'),
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