import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:youme_rtc_engine/youme_rtc_engine.dart';

void main() {
  const MethodChannel channel = MethodChannel('youme_rtc_engine');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await YoumeRtcEngine.platformVersion, '42');
  });
}
