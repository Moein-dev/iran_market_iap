import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Enum representing supported market types
enum MarketType {
  cafebazaar,
  myket,
}

/// Model class for purchase data
class PurchaseData {
  final String productId;
  final String purchaseToken;
  final String orderId;
  final String purchaseTime;
  final String developerPayload;
  final bool isAutoRenewing;
  final String originalJson;
  final String? signature;

  const PurchaseData({
    required this.productId,
    required this.purchaseToken,
    required this.orderId,
    required this.purchaseTime,
    required this.developerPayload,
    required this.isAutoRenewing,
    required this.originalJson,
    this.signature,
  });

  factory PurchaseData.fromMap(Map<String, dynamic> map) {
    return PurchaseData(
      productId: map['productId'] ?? '',
      purchaseToken: map['purchaseToken'] ?? '',
      orderId: map['orderId'] ?? '',
      purchaseTime: map['purchaseTime'] ?? '',
      developerPayload: map['developerPayload'] ?? '',
      isAutoRenewing: map['isAutoRenewing'] ?? false,
      originalJson: map['originalJson'] ?? '',
      signature: map['signature'],
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'productId': productId,
      'purchaseToken': purchaseToken,
      'orderId': orderId,
      'purchaseTime': purchaseTime,
      'developerPayload': developerPayload,
      'isAutoRenewing': isAutoRenewing,
      'originalJson': originalJson,
      'signature': signature,
    };
  }
}

/// Model class for product data
class ProductData {
  final String productId;
  final String title;
  final String description;
  final String price;
  final String priceAmountMicros;
  final String priceCurrencyCode;

  const ProductData({
    required this.productId,
    required this.title,
    required this.description,
    required this.price,
    required this.priceAmountMicros,
    required this.priceCurrencyCode,
  });

  factory ProductData.fromMap(Map<String, dynamic> map) {
    return ProductData(
      productId: map['productId'] ?? '',
      title: map['title'] ?? '',
      description: map['description'] ?? '',
      price: map['price'] ?? '',
      priceAmountMicros: map['priceAmountMicros'] ?? '',
      priceCurrencyCode: map['priceCurrencyCode'] ?? '',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'productId': productId,
      'title': title,
      'description': description,
      'price': price,
      'priceAmountMicros': priceAmountMicros,
      'priceCurrencyCode': priceCurrencyCode,
    };
  }
}

/// Model class for RSA key configuration
class RSAKeyConfig {
  final String publicKey;
  final String? privateKey;
  final String? keyAlias;

  const RSAKeyConfig({
    required this.publicKey,
    this.privateKey,
    this.keyAlias,
  });

  Map<String, dynamic> toMap() {
    return {
      'publicKey': publicKey,
      'privateKey': privateKey,
      'keyAlias': keyAlias,
    };
  }
}

/// Main class for Market IAP operations
class MarketIAP {
  static const MethodChannel _channel = MethodChannel('market_iap');
  static MarketType? _currentMarketType;
  static RSAKeyConfig? _rsaKeyConfig;

  /// Initialize the Market IAP plugin
  /// 
  /// Loads the market type from environment variables and initializes the native implementation
  static Future<bool> initialize() async {
    try {
      // Load environment variables
      await dotenv.load(fileName: '.env');
      
      // Get market type from environment or use default
      final marketTypeString = dotenv.env['MARKET_TYPE'] ?? 'cafebazaar';
      _currentMarketType = MarketType.values.firstWhere(
        (type) => type.name == marketTypeString,
        orElse: () => MarketType.cafebazaar,
      );

      if (kDebugMode) {
        print('MarketIAP: Initializing with market type: ${_currentMarketType!.name}');
      }

      // Initialize native implementation
      final result = await _channel.invokeMethod('init', {
        'market': _currentMarketType!.name,
      });

      if (kDebugMode) {
        print('MarketIAP: Native initialization result: $result');
      }

      // Even if the billing service connection fails, we consider initialization successful
      // The billing operations will handle the connection state
      return result == true;
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP initialization failed: $e');
      }
      return false;
    }
  }

  /// Set RSA key configuration for purchase verification
  /// 
  /// This is required for verifying purchase signatures from both CafeBazaar and Myket
  static void setRSAKeyConfig(RSAKeyConfig config) {
    _rsaKeyConfig = config;
  }

  /// Get the current market type
  static MarketType? get currentMarketType => _currentMarketType;

  /// Get the current RSA key configuration
  static RSAKeyConfig? get currentRSAKeyConfig => _rsaKeyConfig;

  /// Check if billing is supported
  static Future<bool> isBillingSupported() async {
    try {
      final result = await _channel.invokeMethod('isBillingSupported', {
        'market': _currentMarketType!.name,
      });
      
      if (kDebugMode) {
        print('MarketIAP: Billing supported: $result');
      }
      
      return result == true;
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP isBillingSupported failed: $e');
      }
      return false;
    }
  }

  /// Check if the billing service is available (app installed and service accessible)
  static Future<bool> isBillingServiceAvailable() async {
    try {
      // Try to get billing support status
      final isSupported = await isBillingSupported();
      
      if (kDebugMode) {
        print('MarketIAP: Billing service available: $isSupported');
      }
      
      return isSupported;
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP isBillingServiceAvailable failed: $e');
      }
      return false;
    }
  }

  /// Get product details
  static Future<List<ProductData>> getProducts(List<String> productIds) async {
    try {
      final result = await _channel.invokeMethod('getProducts', {
        'market': _currentMarketType!.name,
        'productIds': productIds,
      });

      if (result is List) {
        return result.map((item) => ProductData.fromMap(Map<String, dynamic>.from(item))).toList();
      }
      return [];
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP getProducts failed: $e');
      }
      return [];
    }
  }

  /// Purchase a product
  static Future<PurchaseData?> purchase(String productId, {String? developerPayload}) async {
    try {
      final result = await _channel.invokeMethod('purchase', {
        'market': _currentMarketType!.name,
        'productId': productId,
        'developerPayload': developerPayload,
        'rsaConfig': _rsaKeyConfig?.toMap(),
      });

      if (result != null) {
        return PurchaseData.fromMap(Map<String, dynamic>.from(result));
      }
      return null;
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP purchase failed: $e');
      }
      return null;
    }
  }

  /// Consume a purchase
  static Future<bool> consume(String purchaseToken) async {
    try {
      final result = await _channel.invokeMethod('consume', {
        'market': _currentMarketType!.name,
        'purchaseToken': purchaseToken,
      });
      return result == true;
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP consume failed: $e');
      }
      return false;
    }
  }

  /// Get all purchases
  static Future<List<PurchaseData>> getPurchases() async {
    try {
      final result = await _channel.invokeMethod('getPurchases', {
        'market': _currentMarketType!.name,
        'rsaConfig': _rsaKeyConfig?.toMap(),
      });

      if (result is List) {
        return result.map((item) => PurchaseData.fromMap(Map<String, dynamic>.from(item))).toList();
      }
      return [];
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP getPurchases failed: $e');
      }
      return [];
    }
  }

  /// Get purchase history
  static Future<List<PurchaseData>> getPurchaseHistory() async {
    try {
      final result = await _channel.invokeMethod('getPurchaseHistory', {
        'market': _currentMarketType!.name,
        'rsaConfig': _rsaKeyConfig?.toMap(),
      });

      if (result is List) {
        return result.map((item) => PurchaseData.fromMap(Map<String, dynamic>.from(item))).toList();
      }
      return [];
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP getPurchaseHistory failed: $e');
      }
      return [];
    }
  }

  /// Acknowledge a purchase
  static Future<bool> acknowledgePurchase(String purchaseToken) async {
    try {
      final result = await _channel.invokeMethod('acknowledgePurchase', {
        'market': _currentMarketType!.name,
        'purchaseToken': purchaseToken,
      });
      return result == true;
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP acknowledgePurchase failed: $e');
      }
      return false;
    }
  }

  /// Check if a product is purchased
  static Future<bool> isPurchased(String productId) async {
    try {
      final purchases = await getPurchases();
      return purchases.any((purchase) => purchase.productId == productId);
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP isPurchased failed: $e');
      }
      return false;
    }
  }

  /// Verify purchase signature
  static Future<bool> verifyPurchaseSignature(PurchaseData purchase) async {
    try {
      final result = await _channel.invokeMethod('verifyPurchaseSignature', {
        'market': _currentMarketType!.name,
        'purchaseData': purchase.toMap(),
        'rsaConfig': _rsaKeyConfig?.toMap(),
      });
      return result == true;
    } catch (e) {
      if (kDebugMode) {
        print('MarketIAP verifyPurchaseSignature failed: $e');
      }
      return false;
    }
  }

  /// Get RSA key configuration for a specific market
  static String getRSAKeyForMarket() {
    switch (_currentMarketType) {
      case MarketType.cafebazaar:
        return dotenv.env['BAZAAR_RSA_PUBLIC_KEY'] ?? '';
      case MarketType.myket:
        return dotenv.env['MYKET_RSA_PUBLIC_KEY'] ?? '';
      default:
        return '';
    }
  }
} 