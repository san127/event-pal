EventPal

EventPal is a role-based Android event management application that streamlines the complete event lifecycle—from booking requests to event completion. The project consists of two separate Android applications: a Client App for customers to request and track events, and an Admin App for managing bookings, assignments, tasks, finances, and event progress.

Built using Kotlin, XML, and Firebase, EventPal enables seamless collaboration between administrators, managers, supervisors, accountants, and clients through real-time synchronization.

Features
Client Application
Book events through an intuitive interface.
Track booking approval and event progress.
View event details and updates in real time.
Admin Application

Boss

Approve or reject event booking requests.
Assign managers to approved events.

Manager

Manage event details and schedules.
Assign supervisors.
Create and monitor tasks.

Supervisor

Update event progress.
Complete assigned tasks.

Accountant

View billing and financial information.

General Features
Role-Based Access Control (RBAC).
Real-time synchronization using Firebase Firestore.
Task management with integrated to-do lists.
Event progress tracking.
Expense and billing management.
Secure user authentication.
Technology Stack
Language: Kotlin
UI: XML
IDE: Android Studio
Backend: Firebase
Database: Firebase Firestore
Authentication: Firebase Authentication
Architecture

The project follows a role-based architecture with separate client and administrator applications. Firebase Authentication secures user access, while Firestore provides cloud-based, real-time data synchronization across all users.
