import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_core/firebase_core.dart';
import 'dart:async';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(
    options: FirebaseOptions(
      apiKey: 'AIzaSyDXnR2ToEK4pYw2Oa4BOVYYCzpIsnqaX-4',
      authDomain: 'digitalwellbeingms.firebaseapp.com',
      projectId: 'digitalwellbeingms',
      storageBucket: 'digitalwellbeingms.appspot.com',
      messagingSenderId: '891881982946',
      appId: '1:891881982946:android:8cf5e91ed951d6e7f207bb',
    ),
  );
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: MyTabs(),
    );
  }
}

class MyTabs extends StatefulWidget {
  @override
  _MyTabsState createState() => _MyTabsState();
}

class _MyTabsState extends State<MyTabs> {
  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text('Digital WellBeing Monitoring App'),
          bottom: TabBar(
            tabs: [
              Tab(text: 'View'),
              Tab(text: 'Manage'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            // View tab body
            ViewTab(),
            // Control tab body
            ControlTab(),
          ],
        ),
      ),
    );
  }
}

class ViewTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<QuerySnapshot>(
      stream: FirebaseFirestore.instance.collection('Apps').snapshots(),
      builder: (BuildContext context, AsyncSnapshot<QuerySnapshot> snapshot) {
        if (snapshot.hasError) {
          return Text('Error: ${snapshot.error}');
        }

        if (snapshot.connectionState == ConnectionState.waiting) {
          return Center(
            child: CircularProgressIndicator(),
          );
        }

        if (snapshot.data!.docs.isEmpty) {
          return Center(child: Text('No apps found'));
        }

        return SingleChildScrollView(
          child: Center(
            child: DataTable(
              columns: const <DataColumn>[
                DataColumn(
                  label: Text(
                    'App Name',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
                DataColumn(
                  label: Text(
                    'Usage Time (minutes:seconds)',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
              ],
              rows: snapshot.data!.docs.map((DocumentSnapshot document) {
                Map<String, dynamic> data = document.data() as Map<String, dynamic>;
                return DataRow(
                  cells: <DataCell>[
                    DataCell(Text(data['appName'] ?? 'Unknown')),
                    DataCell(Text((data['usageTime'] ?? 'Unknown').toString())),
                  ],
                );
              }).toList(),
            ),
          ),
        );
      },
    );
  }
}

class ControlTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(8.0),
      child: Align(
        alignment: Alignment.topCenter,
        child: DataTable(
          columns: const <DataColumn>[
            DataColumn(
              label: Text(
                'Control Options',
                style: TextStyle(fontStyle: FontStyle.italic),
              ),
            ),
          ],
          rows: <DataRow>[
            DataRow(
              cells: <DataCell>[
                DataCell(
                  ListTile(
                    title: Text('Set Time Limits'),
                    onTap: () {
                      showFloatingWidget(context);
                    },
                  ),
                ),
              ],
            ),
            DataRow(
              cells: <DataCell>[
                DataCell(
                  ListTile(
                    title: Text('App Blocklist'),
                    onTap: () {
                      showFloatingWidgett(context);
                    },
                  ),
                ),
              ],
            ),
            // Add more controls as needed
          ],
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

  void showFloatingWidgett(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return FloatingWidgett();
      },
    );
  }
}

class FloatingWidget extends StatefulWidget {
  @override
  _FloatingWidgetState createState() => _FloatingWidgetState();
}

class _FloatingWidgetState extends State<FloatingWidget> {
  Future<QuerySnapshot> fetchApps() {
    return FirebaseFirestore.instance.collection('Apps').get();
  }

  void saveTimeLimit(BuildContext context, String appId, String appName, int newTimeLimit) async {
    try {
      await FirebaseFirestore.instance
          .collection('Apps')
          .doc(appId)
          .update({'time_limit': newTimeLimit, 'blocked': false});
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Time limit for $appName updated successfully!'),
        ),
      );
      Timer(Duration(minutes: newTimeLimit), () async {
        await FirebaseFirestore.instance
            .collection('Apps')
            .doc(appId)
            .update({'blocked': true, 'time_limit': 0});
      });
      Navigator.pop(context);
    } catch (error) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to update time limit. Please try again.'),
        ),
      );
    }
  }


  DataRow buildAppTimeLimitField(BuildContext context, DocumentSnapshot app) {
    Map<String, dynamic> data = app.data() as Map<String, dynamic>;
    String appId = app.id;
    String appName = data['appName'] ?? 'Unknown App';
    TextEditingController timeLimitController = TextEditingController();

    return DataRow(
      cells: [
        DataCell(Text(appName, style: TextStyle(fontWeight: FontWeight.bold))),
        DataCell(
          SizedBox(
            width: 100.0,
            child: TextField(
              controller: timeLimitController,
              decoration: InputDecoration(
                hintText: '0000',
                //labelText: 'Time (mins)',
              ),
              keyboardType: TextInputType.number,
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
                LengthLimitingTextInputFormatter(4),
              ],
            ),
          ),
        ),
        DataCell(
          ElevatedButton(
            onPressed: () {
              int newTimeLimit = int.tryParse(timeLimitController.text) ?? 0;
              saveTimeLimit(context, appId, appName, newTimeLimit);
            },
            child: Text('Set'),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<QuerySnapshot>(
      future: fetchApps(),
      builder: (BuildContext context, AsyncSnapshot<QuerySnapshot> snapshot) {
        if (snapshot.hasError) {
          return Text('Something went wrong');
        }

        if (snapshot.connectionState == ConnectionState.waiting) {
          return Text('Loading...');
        }

        return Center(
          child: Material(
            elevation: 4.0,
            borderRadius: BorderRadius.circular(8.0),
            child: Container(
              width: MediaQuery.of(context).size.width * 0.6,
              padding: EdgeInsets.all(20.0),
              color: Colors.white,
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Set Time Limits',
                      style: TextStyle(fontSize: 20.0, fontWeight: FontWeight.bold),
                    ),
                    SizedBox(height: 20.0),
                    DataTable(
                      columns: const <DataColumn>[
                        DataColumn(label: Text('App Name')),
                        DataColumn(label: Text('Time Limit(in minutes)')),
                        DataColumn(label: Text('Set Button')),
                      ],
                      rows: snapshot.data?.docs.map((app) => buildAppTimeLimitField(context, app)).toList() ?? [],
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}


  class FloatingWidgett extends StatelessWidget {
  void updateAppBlockStatus(BuildContext context, String packageName, bool isChecked) async {
    try {
      // Update the document in Firestore
      await FirebaseFirestore.instance
          .collection('Apps')
          .doc(packageName) // Use the package name as the document ID
          .update({'blocked': isChecked}); // Update the 'blocked?' field
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('$packageName ${isChecked ? 'blocked' : 'unblocked'} successfully!'),
        ),
      );
    } catch (error) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to update block status for $packageName. Please try again.'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<QuerySnapshot>(
      stream: FirebaseFirestore.instance.collection('Apps').snapshots(),
      builder: (BuildContext context, AsyncSnapshot<QuerySnapshot> snapshot) {
        if (snapshot.hasError) {
          return Text('Error: ${snapshot.error}');
        }

        if (snapshot.connectionState == ConnectionState.waiting) {
          return Center(
            child: CircularProgressIndicator(),
          );
        }

        if (snapshot.data!.docs.isEmpty) {
          return Center(child: Text('No apps found'));
        }

        return Center(
          child: Material(
            elevation: 4.0,
            borderRadius: BorderRadius.circular(8.0),
            child: Container(
              width: MediaQuery.of(context).size.width * 0.6,
              padding: EdgeInsets.all(20.0),
              color: Colors.white,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'App BlockList',
                    style: TextStyle(fontSize: 20.0, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 20.0),
                  Expanded(
                    child: SingleChildScrollView(
                      scrollDirection: Axis.vertical,
                      child: DataTable(
                        columns: const <DataColumn>[
                          DataColumn(
                            label: Text(
                              'App Name',
                              style: TextStyle(fontStyle: FontStyle.italic),
                            ),
                          ),
                          DataColumn(
                            label: Text(
                              'Blocked?',
                              style: TextStyle(fontStyle: FontStyle.italic),
                            ),
                          ),
                        ],
                        rows: snapshot.data!.docs.map((document) {
                          Map<String, dynamic> data = document.data() as Map<String, dynamic>;
                          return DataRow(
                            cells: <DataCell>[
                              DataCell(Text(data['appName'] ?? 'Unknown')),
                              DataCell(Checkbox(
                                value: data['blocked'] ?? false,
                                onChanged: (bool? value) {
                                  // Call the updateAppBlockStatus function to update Firestore
                                  updateAppBlockStatus(context, document.id, value ?? false);
                                },
                              )),
                            ],
                          );
                        }).toList(),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
