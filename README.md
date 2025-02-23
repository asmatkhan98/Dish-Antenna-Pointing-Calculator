# Dish Antenna Pointing Calculator

## Overview
The **Dish Antenna Pointing Calculator** is an Android application developed in AndroidIDE. It helps users accurately align their satellite dish antennas by calculating essential parameters such as azimuth, elevation, and LNB skew based on location and satellite data.

## Features
- **Focal Point Calculation:** Determines the focal point of a dish using diameter and depth.
- **Satellite Antenna Alignment:** Computes azimuth, elevation, and LNB skew based on user-inputted satellite longitude and location.
- **GPS Integration:** Retrieves the user's latitude and longitude for precise calculations.
- **Compass & Elevation Display:** Provides real-time azimuth and elevation angles for accurate dish pointing.

## Installation
### Prerequisites
- Android device running Android 5.0 (Lollipop) or higher.
- AndroidIDE installed for manual modifications (if necessary).

### Steps
1. Download the APK file from the provided source.
2. Install the APK on your Android device (enable "Install from Unknown Sources" if required).
3. Open the app and grant necessary permissions (GPS access).

## Usage Guide
### **1. Dish Focal Point Calculator**
1. Select the **Measurement Unit** from the dropdown.
2. Enter **Dish Diameter** and **Dish Depth**.
3. Tap **Calculate** to get the Focal Point value.

### **2. Satellite Antenna Pointing**
1. Enter **Satellite Longitude** and select the direction (East/West).
2. Tap **Get My Location** to fetch latitude and longitude automatically.
3. Tap **Calculate** to obtain azimuth, elevation, and LNB skew values.

### **3. Compass & Elevation**
- The compass provides real-time azimuth direction.
- Elevation data updates dynamically based on device tilt.

## Permissions Required
- **GPS/Location:** Required to fetch accurate latitude and longitude.
- **Sensor Access:** Utilized for compass functionality.

## Code Structure
### **Main Files**
- `MainActivity.kt`: Contains core logic for calculation and UI interaction.
- `activity_main.xml`: Defines the user interface layout.

### **Key UI Components**
- **Spinners:** Allow unit selection.
- **EditText Fields:** For manual input of values.
- **Buttons:** Trigger calculations and location fetching.
- **TextViews:** Display results dynamically.
- **Compass ImageView:** Shows real-time directional updates.

## Future Enhancements
- **Offline Mode:** Allow calculations without GPS dependency.
- **Augmented Reality (AR) Integration:** Provide a visual overlay for dish alignment.
- **Additional Satellite Database:** Preload satellite data for quicker calculations.

## Developer
- **Developer:** Asmatkhan98
- **Developed Using:** AndroidIDE (Android)

For support or feature requests, contact the developer or contribute to the project repository.

---
**Disclaimer:** This app is intended for informational purposes only. Users should verify calculations with professional satellite alignment tools if needed.

