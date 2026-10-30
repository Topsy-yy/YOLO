# 🛣️ Road Damage Detector

An Android-based road damage detection and reporting application powered by on-device machine learning. The application uses a custom YOLO TensorFlow Lite model to identify road anomalies, including potholes and cracks, through live camera detection or selected gallery images.

Beyond detection, the application provides an end-to-end reporting workflow that captures damage evidence, records GPS coordinates, stores reports locally, and allows users to review previously submitted reports.

## ✨ Features

- **Real-Time Road Damage Detection:** Uses a custom YOLO `.tflite` model to identify road damage and display predictions using bounding boxes.
- **Camera and Gallery Support:** Detect road anomalies using a live camera feed or images selected from the device gallery.
- **Damage Reporting:** Generates reports containing captured images, AI predictions, GPS coordinates, severity levels, and user-provided notes.
- **Offline-First Architecture:** Stores reports and associated images locally using SQLite and internal application storage.
- **Report History:** Provides a chronological view of previously saved reports.
- **Report Details:** Displays high-resolution images, detection overlays, and detailed report metadata.
- **Report Management:** Allows users to safely delete locally stored reports.
- **Dashboard:** Provides centralized navigation to the application's main features.

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Kotlin |
| Platform | Android |
| User Interface | XML Layouts, Material Components |
| UI Architecture | View Binding, RecyclerView |
| Machine Learning | TensorFlow Lite |
| Object Detection | YOLO |
| Local Database | SQLite |
| Image Storage | Android Internal Storage |
| Location Services | Google Play Services Fused Location Provider |
| Asynchronous Operations | Kotlin Coroutines |
| Serialization | Gson |

## 📋 Prerequisites

Before setting up the project, ensure you have:

- **Android Studio:** Latest stable version recommended.
- **JDK:** Java Development Kit 17.
- **Android SDK:** API Level 34 (Android 14).
- **Android Device:** Android API Level 24 or higher.
- **USB Debugging:** Enabled on your physical Android device.

An emulator can be used for interface testing. However, a physical Android device is recommended for testing camera functionality, GPS acquisition, and on-device inference.

## 🚀 Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/RoadDamageDetector.git

cd RoadDamageDetector
```

Replace `YOUR_USERNAME` with the GitHub username or organization hosting the repository.

### 2. Open the Project

1. Launch Android Studio.
2. Select **File → Open**.
3. Navigate to the cloned repository.
4. Open the project.
5. Allow Gradle to complete synchronization and dependency resolution.

### 3. Configure the Machine Learning Model

The application requires a TensorFlow Lite model named:

```text
best.tflite
```

Place the model inside:

```text
app/src/main/assets/best.tflite
```

The current detection implementation expects the following model dimensions:

**Input shape:**
```text
[1, 3, 640, 640]
```

**Output shape:**
```text
[1, 13, 8400]
```

Ensure that the model's input and output formats match the preprocessing and postprocessing implementation in `TFLiteDetector.kt`.

If you replace or retrain the model, the preprocessing, tensor handling, output decoding, and Non-Maximum Suppression (NMS) logic may need to be updated accordingly.

### 4. Application Permissions

The application requires the following permissions:

- **Camera:** Required for live road damage detection.
- **Fine Location:** Used to obtain accurate GPS coordinates for reports.
- **Coarse Location:** Provides approximate location information when precise location is unavailable.

Permissions are requested at runtime when the relevant functionality is accessed.

### 5. Build and Run

1. Connect your Android device using USB.
2. Enable USB debugging.
3. Select your connected device in Android Studio.
4. Click the **Run (▶)** button.
5. Wait for the application to build and install.

Alternatively, build the project from the terminal:

```bash
./gradlew assembleDebug
```

## 🏗️ Application Architecture

The application follows a modular Android architecture that separates user interface operations, machine learning inference, local persistence, and location services.

### User Interface Layer
- Traditional Android XML layouts.
- Material Components and MaterialCardView.
- RecyclerView for displaying report history.
- View Binding for accessing UI components.

### Machine Learning Layer
- TensorFlow Lite Interpreter for on-device inference.
- Custom YOLO object detection model.
- Manual CHW tensor preprocessing.
- Bounding box decoding and confidence filtering.
- Non-Maximum Suppression (NMS) to eliminate overlapping predictions.

### Data Persistence Layer
- SQLiteOpenHelper for structured report storage.
- Android internal `filesDir` for storing report images.
- Gson for serialization and deserialization where required.

### Location Layer
- Google Play Services FusedLocationProviderClient.
- GPS coordinate acquisition for report geotagging.
- Location fallback handling.

### Background Operations
- Kotlin Coroutines for asynchronous operations.
- `lifecycleScope` for lifecycle-aware tasks.
- `Dispatchers.IO` for database and file operations to avoid blocking the main thread.

## 🧪 Testing

### Unit Testing

Run the available JVM unit tests using:

```bash
./gradlew test
```

The current tests cover areas including database serialization and location fallback logic.

### Device Testing

Physical device testing is recommended to validate:

- Camera initialization and image capture.
- Real-time model inference.
- Detection accuracy under different lighting conditions.
- GPS coordinate acquisition.
- Report creation and persistence.
- Report history and detail viewing.
- Offline functionality.

## ⚠️ Known Limitations

- **Cloud Synchronization:** Reports are currently stored locally as `LOCAL_DRAFT`. Remote synchronization and centralized reporting are not yet implemented.
- **Map Integration:** The dashboard map functionality is currently a placeholder. Full implementation requires integration with a mapping service and the appropriate API configuration.
- **Detection Accuracy:** Model performance depends on training data, environmental conditions, image quality, and lighting.
- **Device Performance:** Inference speed may vary depending on the Android device's processing capabilities.

## 🔬 Dataset and Research Attribution

The machine learning model used in this application was trained using the **Road Damage Dataset 2022 (RDD2022)**.

We acknowledge the researchers and contributors responsible for collecting, annotating, and making the dataset available for road damage detection research.

**Dataset Repository:**  
https://github.com/sekilab/RoadDamageDetector

**Related Research Publication:**

Arya, D., Maeda, H., Ghosh, S. K., Toshniwal, D., Mraz, A., Kashiyama, T., & Sekimoto, Y. (2021). *Global Road Damage Detection: State-of-the-Art Solutions with the RDD2020 Dataset.* arXiv preprint arXiv:2111.10933.

## 📄 License

The licensing terms applicable to this project are provided in the [`LICENSE`](LICENSE) file.

The RDD2022 dataset and associated research materials remain subject to their respective licensing and attribution requirements. Refer to the original dataset repository for details.

## 🤝 Contributions

Contributions, bug reports, and suggestions for improving the application are welcome.

If you would like to contribute:

1. Fork the repository.
2. Create a feature branch.
3. Implement your changes.
4. Test your implementation.
5. Submit a pull request describing your changes.

---

**Road Damage Detector**  
*Leveraging on-device artificial intelligence to support road condition monitoring and reporting.*
