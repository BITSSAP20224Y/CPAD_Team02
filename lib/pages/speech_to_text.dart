import 'package:flutter/material.dart';
import 'package:speech_to_text/speech_to_text.dart';

class SpeechToTextScreen extends StatefulWidget {
  const SpeechToTextScreen({super.key});

  @override
  State<SpeechToTextScreen> createState() => _SpeechToTextScreenState();
}

class _SpeechToTextScreenState extends State<SpeechToTextScreen> {
  final SpeechToText _speechToText = SpeechToText();
  bool _speechEnabled = false;
  String _wordSpoken = "";
  double _confidenceLevel = 0.0;
  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    initSpeech();
  }

  void initSpeech() async {
    _speechEnabled = await _speechToText.initialize();
    setState(() {});
  }

  void _startListening() async {
    await _speechToText.listen(onResult: _onSpeechReport);
    setState(() {
      _confidenceLevel = 0.0;
    });
  }

  void _stopListening() async {
    await _speechToText.stop();
    setState(() {});
  }

  void _onSpeechReport(result) {
    _wordSpoken = "${result.recognizedWords}";
    _confidenceLevel = result.confidence;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Speech to text',
          style: TextStyle(color: Colors.amber, fontFamily: 'Quicksand'),
        ),
      ),
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (_speechToText.isNotListening && _confidenceLevel > 0)
              Text(
                'Confidence ${(_confidenceLevel * 100).toStringAsFixed(1)}%',
                style: const TextStyle(
                    color: Colors.amber, fontFamily: 'Quicksand', fontSize: 18),
              ),
            Expanded(
                child: Container(
              padding: const EdgeInsets.all(16),
              child: Text(_wordSpoken,
                  style: const TextStyle(
                      color: Colors.amber,
                      fontFamily: 'Quicksand',
                      fontSize: 18)),
            )),
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Text(
                _speechToText.isListening
                    ? "listening"
                    : _speechEnabled
                        ? "Tap to start recording "
                        : "Speech not available",
                style: const TextStyle(
                    color: Colors.amber, fontFamily: 'Quicksand', fontSize: 18),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 20),
              child: IconButton(
                  onPressed: (_speechToText.isListening
                      ? _stopListening
                      : _startListening),
                  icon: Container(
                      decoration: BoxDecoration(
                          color: Colors.amberAccent,
                          borderRadius: BorderRadius.circular(10)),
                      child: Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Icon(
                          _speechToText.isNotListening
                              ? Icons.mic_off
                              : Icons.mic,
                          color: Colors.black,
                        ),
                      ))),
            )
          ],
        ),
      ),
    );
  }
}
