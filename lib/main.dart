import 'package:flutter/material.dart';
import 'package:mlapp/pages/main_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'ML kit',
      themeMode: ThemeMode.dark,
      theme: ThemeData(
        brightness: Brightness.dark,
        textTheme: Typography.blackMountainView.apply(fontFamily: 'Quicksand'),
      ),
      //home: const SpeechToTextScreen(),
      home: const ImagePickerScreen(),
    );
  }
}
