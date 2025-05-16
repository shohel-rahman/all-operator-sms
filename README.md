## Overview
This service integrates with multiple mobile operator APIs to send SMS messages, track delivery status, and maintain comprehensive records. It provides a unified interface for sending messages across different 
operators while handling the complexity of vendor-specific API implementations.

## Features
**Multi-Operator Support:** Seamlessly routes SMS through appropriate carrier APIs based on recipient numbers

**Asynchronous Processing:** Efficiently pulls pending SMS from database queue for dispatch

**Database Integration:** Stores all messages and their statuses for auditing and reporting

**Delivery Tracking:** Automatically polls carrier APIs to update delivery status of sent messages

**Response Handling:** Processes and stores API responses for troubleshooting and analytics

**Fault Tolerance:** Implements retry mechanisms for failed delivery attempts

**Scalable Architecture:** Designed to handle high-volume messaging requirements

## Technology Stack
- Spring Boot framework
- RESTful API integration
- Database persistence (Oracle)
- Scheduled tasks for status polling
- Comprehensive logging and monitoring

This application serves as a robust middleware solution for organizations requiring reliable SMS delivery across multiple carrier networks with full tracking capabilities.
