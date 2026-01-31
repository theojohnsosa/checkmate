# Checkmate - Classroom Attendance Tracker App

A mobile-first attendance tracking application built with Android that streamlines classroom check-ins while maintaining security and accuracy. Checkmate simplifies attendance management for educational institutions with role-based access and intelligent fraud detection.

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=java&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![API Level](https://img.shields.io/badge/API%20Level-21%2B-brightgreen?style=flat-square)

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Key Components](#key-components)
- [Contributing](#contributing)
- [License](#license)

## Features

### For Teachers (Admins)

- **Class Management** - Create and manage multiple classes with intuitive interfaces
- **Attendance Sessions** - Enable attendance tracking with configurable time limits and grace periods
- **Student Management** - Add, remove, and organize students within classes
- **Seat Planning** - Visual seat arrangement tools for classroom management
- **Session Monitoring** - View real-time attendance status and detailed session information
- **Archive Management** - Archive or remove classes and sessions with confirmation dialogs
- **Attendance Analytics** - Track attendance streaks and historical attendance data

### For Students

- **Class Access** - Join assigned classes through simple join dialogs
- **Attendance Marking** - Quick and intuitive attendance check-in during active sessions
- **History Tracking** - View detailed attendance history and statistics
- **Streak Monitoring** - Track attendance streaks to encourage consistency
- **Profile Management** - Manage personal profile and settings

### Intelligent Attendance System

- **Grace Period Support** - Automatic handling of late arrivals with configurable grace windows
- **False Check-in Detection** - Smart validation to prevent duplicate or fraudulent attendance submissions
- **Real-time Status Updates** - Live attendance status monitoring during sessions
- **Session Management** - Remove sessions with confirmation to prevent accidental data loss
- **Comprehensive Analytics** - Detailed attendance cards, history models, and streak tracking

## Architecture

Checkmate follows a layered architecture pattern with clear separation of concerns:

```
Activities (UI Layer)
    ↓
Adapters (Data Presentation)
    ↓
Models (Data Layer)
    ↓
Dialogs (User Interactions)
```

The app uses an Activity-based architecture with Fragment support through Adapters for efficient list rendering and RecyclerViews for scalable UI components.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java |
| **Platform** | Android (Native) |
| **UI Framework** | Android XML Layouts |
| **Design Pattern** | Material Design |
| **Min API Level** | 21 |
| **Architecture** | Activity & Adapter Pattern |

## Installation

### Prerequisites

- Android Studio (latest version)
- Java Development Kit (JDK 8 or higher)
- Android SDK (API 21 or higher)
- Gradle 7.0 or higher

### Steps

1. Clone the repository:
```bash
git clone https://github.com/theojohnsosa/checkmate.git
cd checkmate
```

2. Open the project in Android Studio:
   - File → Open → Select the checkmate folder
   - Let Android Studio sync the Gradle files

3. Configure your Android device or emulator:
   - Set up an emulator via AVD Manager or connect a physical device
   - Ensure USB debugging is enabled (for physical devices)

4. Build and run the app:
   ```bash
   ./gradlew build
   ```

5. Deploy to your device:
   - Click the "Run" button in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

## Usage

### For Teachers

1. **Sign Up & Log In** - Create an account and sign in as a teacher
2. **Create a Class** - Click the create class button and fill in class details
3. **Add Students** - Go to class details and add students by email or ID
4. **Enable Attendance** - Create an attendance session with a start time and duration
5. **Set Grace Period** - Configure the grace period for late arrivals
6. **Monitor Attendance** - View real-time attendance status during the session
7. **View Analytics** - Check attendance history and streaks

### For Students

1. **Sign Up & Log In** - Create an account and sign in as a student
2. **Join Class** - Use the join class dialog with the class code provided by teacher
3. **Mark Attendance** - During an active attendance session, tap "Check In"
4. **View History** - Navigate to attendance history to see past records and streaks
5. **Check Profile** - Update your profile information in settings

## Project Structure

```
checkmate/
├── java/com/example/authtest/
│   ├── Activities/
│   │   ├── SignIn.java
│   │   ├── SignUp.java
│   │   ├── SplashScreen.java
│   │   ├── TeacherHome.java
│   │   ├── StudentHome.java
│   │   ├── CreateClass.java
│   │   ├── SessionDetailsActivity.java
│   │   ├── AttendanceHistoryActivity.java
│   │   ├── SeatPlan.java
│   │   ├── SettingsActivity.java
│   │   ├── ProfilePage.java
│   │   └── ArchiveActivity.java
│   ├── Adapters/
│   │   ├── ClassAdapter.java
│   │   ├── StudentAttendanceAdapter.java
│   │   ├── RecentSessionAdapter.java
│   │   └── AttendanceHistoryAdapter.java
│   ├── Models/
│   │   ├── ClassModel.java
│   │   ├── StudentAttendanceModel.java
│   │   └── AttendanceHistoryModel.java
│   └── Dialogs/
│       ├── JoinClassDialog.java
│       ├── AddStudentsForm.java
│       ├── RemoveClassConfirmationDialog.java
│       ├── RemoveSessionConfirmationDialog.java
│       ├── RemoveStudentConfirmationDialog.java
│       ├── ArchiveClassConfirmationDialog.java
│       └── LogoutConfirmationDialog.java
├── res/layout/
│   ├── activity_*.xml
│   ├── dialog_*.xml
│   ├── *_card.xml
│   └── *_item.xml
└── AndroidManifest.xml
```

## Key Components

### Activities

- **SignIn/SignUp** - Authentication with role selection (Teacher/Student)
- **SplashScreen** - App initialization and launcher activity
- **TeacherHome** - Teacher dashboard with class management
- **StudentHome** - Student dashboard with joined classes
- **CreateClass** - Class creation form
- **SessionDetailsActivity** - Detailed attendance session view
- **AttendanceHistoryActivity** - Attendance records with filtering
- **SeatPlan** - Visual classroom layout
- **SettingsActivity** - User preferences and app configuration
- **ProfilePage** - User profile management
- **ArchiveActivity** - Archived classes and sessions management

### Adapters

- **ClassAdapter** - Displays list of classes in RecyclerView
- **StudentAttendanceAdapter** - Shows attendance list for a session
- **RecentSessionAdapter** - Displays recent attendance sessions
- **AttendanceHistoryAdapter** - Lists historical attendance records

### Models

- **ClassModel** - Represents classroom data
- **StudentAttendanceModel** - Attendance record for a student
- **AttendanceHistoryModel** - Historical attendance data
- **AttendanceStreak** - Tracks consecutive attendance

### Dialogs

- **JoinClassDialog** - Class joining interface
- **AddStudentsForm** - Student addition form
- **Confirmation Dialogs** - Safe operations for delete/remove actions
- **AppIconSelectionDialog** - Custom app icon selection

## Contributing

Contributions are welcome! To contribute to Checkmate:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please ensure your code follows the existing code style and includes appropriate comments.

## Future Enhancements

- Cloud database integration (Firebase)
- Biometric authentication
- Push notifications for attendance reminders
- Parent/Guardian portal
- Detailed analytics dashboard
- Offline mode support
- QR code-based check-in

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Authors

- **Theo John Sosa** - Initial work - [theojohnsosa](https://github.com/theojohnsosa)

## Support

For issues, feature requests, or questions, please open an issue on the [GitHub Issues](https://github.com/theojohnsosa/checkmate/issues) page.

---

Made with ❤️ by Theo John Sosa
