import 'package:flutter/material.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Floating Widget Example'),
      ),
      body: Center(
        child: ElevatedButton(
          onPressed: () {
            showFloatingWidget(context);
          },
          child: Text('Show Floating Widget'),
        ),
      ),
    );
  }

  void showFloatingWidget(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return FloatingWidget();
      },
    );
  }
}

class FloatingWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        // Background overlay with blur effect
        GestureDetector(
          onTap: () {
            Navigator.pop(context);
          },
          child: Container(
            color: Colors.black54,
          ),
        ),
        // Floating widget
        Center(
          child: Material(
            elevation: 4.0,
            borderRadius: BorderRadius.circular(8.0),
            child: Container(
              padding: EdgeInsets.all(20.0),
              color: Colors.white,
              child: Text('Floating Widget'),
            ),
          ),
        ),
      ],
    );
  }
}
