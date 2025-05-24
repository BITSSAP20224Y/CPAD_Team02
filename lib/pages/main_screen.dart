import "dart:io";
import "package:flutter/material.dart";
import "package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart";
import "package:image_picker/image_picker.dart";
import "package:mlapp/pages/speech_to_text.dart";
import "package:mlapp/pages/text_styles.dart";

class ImagePickerScreen extends StatefulWidget {
  const ImagePickerScreen({super.key});

  @override
  State<ImagePickerScreen> createState() => _ImagePickerScreenState();
}

class _ImagePickerScreenState extends State<ImagePickerScreen> {
  File? pickedImage;
  late String textData;
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Center(
          child: Text(
            'Multi Modal Recognition',
            style: TextStyle(color: Colors.amber, fontFamily: 'Quicksand'),
          ),
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Center(
                child: pickedImage != null
                    ? Container(
                        decoration: const BoxDecoration(
                          color: Colors.amber,
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(8.0),
                          child: Image.file(
                            pickedImage!,
                          ),
                        ),
                      )
                    : const Text(
                        'Select the action to perform',
                        style: TextStyle(color: Colors.amber),
                      ),
              ),
            ),
          ),
          if (pickedImage != null)
            Expanded(
                child: Center(
              child: FutureBuilder(
                  future: _extractString(),
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      return Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Container(
                          decoration: BoxDecoration(
                              color: Colors.black38,
                              borderRadius: BorderRadius.circular(20)),
                          child: SingleChildScrollView(
                            child: SelectionArea(
                              child: Padding(
                                padding: const EdgeInsets.all(12.0),
                                child: Text(
                                  snapshot.requireData.isEmpty
                                      ? 'No text detected'
                                      : textData = snapshot.requireData
                                          .replaceAll('\n', ' '),
                                  textAlign: TextAlign.left,
                                  style: quickSand
                                      .merge(regularFont)
                                      .copyWith(fontSize: 14),
                                ),
                              ),
                            ),
                          ),
                        ),
                      );
                    }
                    if (snapshot.hasError) {
                      return const Text("Error while detecting");
                    }
                    return const Text('Detecting text...');
                  }),
            )),
          Padding(
            padding: const EdgeInsets.only(bottom: 30),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                IconButton(
                    onPressed: () async {
                      final imagePicker = ImagePicker();
                      final image = await imagePicker.pickImage(
                          source: ImageSource.camera);
                      if (image == null) return;
                      setState(() {
                        pickedImage = File(image.path);
                      });
                    },
                    icon: Container(
                        decoration: BoxDecoration(
                            color: Colors.amberAccent,
                            borderRadius: BorderRadius.circular(10)),
                        child: const Padding(
                          padding: EdgeInsets.all(8.0),
                          child: Icon(
                            Icons.camera_alt_outlined,
                            color: Colors.black,
                          ),
                        ))),
                IconButton(
                    onPressed: () {
                      setState(() {
                        pickedImage = null;
                        textData = "";
                      });
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (context) => const SpeechToTextScreen(),
                        ),
                      );
                    },
                    icon: Container(
                        decoration: BoxDecoration(
                            color: Colors.amberAccent,
                            borderRadius: BorderRadius.circular(10)),
                        child: const Padding(
                          padding: EdgeInsets.all(8.0),
                          child: Icon(
                            Icons.mic,
                            color: Colors.black,
                          ),
                        ))),
                IconButton(
                    onPressed: () async {
                      final imagePicker = ImagePicker();
                      final image = await imagePicker.pickImage(
                          source: ImageSource.gallery);
                      if (image == null) return;
                      setState(() {
                        pickedImage = File(image.path);
                      });
                    },
                    icon: Container(
                        decoration: BoxDecoration(
                            color: Colors.amberAccent,
                            borderRadius: BorderRadius.circular(10)),
                        child: const Padding(
                          padding: EdgeInsets.all(8.0),
                          child: Icon(
                            Icons.photo_sharp,
                            color: Colors.black,
                          ),
                        )))
              ],
            ),
          )
        ],
      ),
    );
  }

//child:
  Future<String> _extractString() async {
    final InputImage inputImage = InputImage.fromFile(pickedImage!);
    final textRecognizer = TextRecognizer(script: TextRecognitionScript.latin);
    final RecognizedText recognizedText =
        await textRecognizer.processImage(inputImage);
    textRecognizer.close();
    return recognizedText.text;
  }
}
