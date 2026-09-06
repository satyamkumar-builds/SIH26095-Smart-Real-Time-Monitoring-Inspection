# Smart Real-Time Monitoring & Inspection Portal

A web-based monitoring and inspection portal designed for managing,
reviewing, tracking, and monitoring field inspection activities.

The portal is intended for supervisory, administrative, reviewer,
and monitoring roles involved in the inspection workflow.

---

## Project Overview

The system supports the complete inspection monitoring workflow,
starting from inspection assignment and continuing through field
inspection, evidence submission, review, corrective actions,
re-inspection, verification, and closure.

The web portal acts as the monitoring and management interface,
while field-level inspection activities are handled through the
mobile application.

---

## Web Portal

The web portal provides interfaces for:

- Inspection dashboard
- Inspection register
- Inspection assignment
- Inspection details
- Evidence and records
- Location review
- Issues and corrective actions
- Re-inspection
- Live monitoring requests
- Random video conferencing requests
- Analytics
- Reports
- Notifications
- User and role management
- System logs
- Support requests

---

## Inspection Workflow

The main inspection workflow is:

Assignment
    ↓
Field Inspection
    ↓
Evidence Submission
    ↓
Review
    ↓
Findings / Issues
    ↓
Corrective Action
    ↓
Re-inspection
    ↓
Verification
    ↓
Closure

---

## Technology Used

### Frontend

- HTML5
- CSS3
- JavaScript
- React
- Vite

### Backend

- Supabase
- PostgreSQL
- Supabase Authentication
- Row Level Security (RLS)

---

## Project Structure

text
SIH26095-Smart-Real-Time-Monitoring-Inspection/
│
├── README.md
│
├── web/
│   ├── 01-login.html
│   ├── 02-index.html
│   ├── 03-inspections.html
│   ├── 04-inspection-assignment.html
│   ├── 05-inspection-detail.html
│   ├── 06-evidence.html
│   ├── 07-location-review.html
│   ├── 08-issues.html
│   ├── 09-reinspection.html
│   ├── 10-live-monitoring.html
│   ├── 11-random-vc.html
│   ├── 12-analytics.html
│   ├── 13-reports.html
│   ├── 14-notifications.html
│   ├── 15-users-roles.html
│   ├── 16-system-logs.html
│   ├── 17-support.html
│   ├── 18-style.css
│   └── 19-site.js
│
└── backend/
    └── ...


---

Web Folder

The web folder contains the frontend portal.

The HTML files represent different portal modules.

The CSS file contains the complete visual styling of the portal.

The JavaScript file contains the frontend interaction and application logic.


---

Backend

The backend is planned using Supabase and PostgreSQL.

The backend is responsible for areas such as:

Authentication

Database storage

User roles

Inspection records

Evidence metadata

Issues

Corrective actions

Re-inspection records

Notifications

Audit logs

Access control




---

Development Status

Completed

Web portal structure

Inspection monitoring screens

Inspection workflow screens

Evidence management interface

Issue management interface

Re-inspection interface

Monitoring and reporting interfaces

Responsive frontend styling

Frontend interaction logic


In Progress

Supabase backend integration

Authentication integration

Database integration

Evidence storage

Production-level access control


Planned

Mobile application integration

Real-time monitoring integration

Video conferencing integration

Production deployment

Advanced analytics

Complete audit and compliance workflow





---

Important Note

This repository is developed as a software project for implementing a real-time monitoring and inspection workflow.

Operational records, government records, inspection results, locations, evidence, and other production information should only come from authorized system data sources.

No real operational data is included in the frontend.


---

License

This project is intended for development and demonstration purposes.
