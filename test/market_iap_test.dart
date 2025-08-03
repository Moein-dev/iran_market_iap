import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:iran_market_iap/market_iap.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('MarketIAP Tests', () {
    const MethodChannel channel = MethodChannel('market_iap');

    setUp(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
        switch (methodCall.method) {
          case 'init':
            return true;
          case 'isBillingSupported':
            return true;
          case 'getProducts':
            return [
              {
                'productId': 'test_product',
                'title': 'Test Product',
                'description': 'A test product',
                'price': '1.99',
                'priceAmountMicros': '1990000',
                'priceCurrencyCode': 'USD',
              }
            ];
          case 'purchase':
            return {
              'productId': 'test_product',
              'purchaseToken': 'test_token',
              'orderId': 'test_order',
              'purchaseTime': '1234567890',
              'developerPayload': 'test_payload',
              'isAutoRenewing': false,
              'originalJson': '{"test": "data"}',
              'signature': 'test_signature',
            };
          case 'consume':
            return true;
          case 'getPurchases':
            return [
              {
                'productId': 'test_product',
                'purchaseToken': 'test_token',
                'orderId': 'test_order',
                'purchaseTime': '1234567890',
                'developerPayload': 'test_payload',
                'isAutoRenewing': false,
                'originalJson': '{"test": "data"}',
                'signature': 'test_signature',
              }
            ];
          case 'getPurchaseHistory':
            return [
              {
                'productId': 'test_product',
                'purchaseToken': 'test_token',
                'orderId': 'test_order',
                'purchaseTime': '1234567890',
                'developerPayload': 'test_payload',
                'isAutoRenewing': false,
                'originalJson': '{"test": "data"}',
                'signature': 'test_signature',
              }
            ];
          case 'acknowledgePurchase':
            return true;
          case 'verifyPurchaseSignature':
            return true;
          default:
            return null;
        }
      });
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    test('MarketType enum values', () {
      expect(MarketType.cafebazaar.name, equals('cafebazaar'));
      expect(MarketType.myket.name, equals('myket'));
    });

    test('PurchaseData fromMap and toMap', () {
      final map = {
        'productId': 'test_product',
        'purchaseToken': 'test_token',
        'orderId': 'test_order',
        'purchaseTime': '1234567890',
        'developerPayload': 'test_payload',
        'isAutoRenewing': false,
        'originalJson': '{"test": "data"}',
        'signature': 'test_signature',
      };

      final purchaseData = PurchaseData.fromMap(map);
      expect(purchaseData.productId, equals('test_product'));
      expect(purchaseData.purchaseToken, equals('test_token'));
      expect(purchaseData.orderId, equals('test_order'));
      expect(purchaseData.purchaseTime, equals('1234567890'));
      expect(purchaseData.developerPayload, equals('test_payload'));
      expect(purchaseData.isAutoRenewing, equals(false));
      expect(purchaseData.originalJson, equals('{"test": "data"}'));
      expect(purchaseData.signature, equals('test_signature'));

      final backToMap = purchaseData.toMap();
      expect(backToMap, equals(map));
    });

    test('ProductData fromMap and toMap', () {
      final map = {
        'productId': 'test_product',
        'title': 'Test Product',
        'description': 'A test product',
        'price': '1.99',
        'priceAmountMicros': '1990000',
        'priceCurrencyCode': 'USD',
      };

      final productData = ProductData.fromMap(map);
      expect(productData.productId, equals('test_product'));
      expect(productData.title, equals('Test Product'));
      expect(productData.description, equals('A test product'));
      expect(productData.price, equals('1.99'));
      expect(productData.priceAmountMicros, equals('1990000'));
      expect(productData.priceCurrencyCode, equals('USD'));

      final backToMap = productData.toMap();
      expect(backToMap, equals(map));
    });

    test('RSAKeyConfig toMap', () {
      final config = RSAKeyConfig(
        publicKey: 'test_public_key',
        privateKey: 'test_private_key',
        keyAlias: 'test_alias',
      );

      final map = config.toMap();
      expect(map['publicKey'], equals('test_public_key'));
      expect(map['privateKey'], equals('test_private_key'));
      expect(map['keyAlias'], equals('test_alias'));
    });

    test('MarketIAP.initialize returns true', () async {
      final result = await MarketIAP.initialize();
      expect(result, isTrue);
    });

    test('MarketIAP.isBillingSupported returns true', () async {
      final result = await MarketIAP.isBillingSupported();
      expect(result, isTrue);
    });

    test('MarketIAP.getProducts returns list', () async {
      final products = await MarketIAP.getProducts(['test_product']);
      expect(products, isA<List<ProductData>>());
      expect(products.length, equals(1));
      expect(products.first.productId, equals('test_product'));
      expect(products.first.title, equals('Test Product'));
    });

    test('MarketIAP.purchase returns PurchaseData', () async {
      final purchase = await MarketIAP.purchase('test_product');
      expect(purchase, isA<PurchaseData>());
      expect(purchase?.productId, equals('test_product'));
      expect(purchase?.purchaseToken, equals('test_token'));
      expect(purchase?.signature, equals('test_signature'));
    });

    test('MarketIAP.consume returns true', () async {
      final result = await MarketIAP.consume('test_token');
      expect(result, isTrue);
    });

    test('MarketIAP.getPurchases returns list', () async {
      final purchases = await MarketIAP.getPurchases();
      expect(purchases, isA<List<PurchaseData>>());
      expect(purchases.length, equals(1));
      expect(purchases.first.productId, equals('test_product'));
      expect(purchases.first.signature, equals('test_signature'));
    });

    test('MarketIAP.getPurchaseHistory returns list', () async {
      final history = await MarketIAP.getPurchaseHistory();
      expect(history, isA<List<PurchaseData>>());
      expect(history.length, equals(1));
      expect(history.first.productId, equals('test_product'));
      expect(history.first.signature, equals('test_signature'));
    });

    test('MarketIAP.acknowledgePurchase returns true', () async {
      final result = await MarketIAP.acknowledgePurchase('test_token');
      expect(result, isTrue);
    });

    test('MarketIAP.verifyPurchaseSignature returns true', () async {
      final purchase = PurchaseData(
        productId: 'test_product',
        purchaseToken: 'test_token',
        orderId: 'test_order',
        purchaseTime: '1234567890',
        developerPayload: 'test_payload',
        isAutoRenewing: false,
        originalJson: '{"test": "data"}',
        signature: 'test_signature',
      );
      
      final result = await MarketIAP.verifyPurchaseSignature(purchase);
      expect(result, isTrue);
    });

    test('MarketIAP.isPurchased returns true for purchased product', () async {
      final result = await MarketIAP.isPurchased('test_product');
      expect(result, isTrue);
    });
  });
} 