# Github Alerts Project 2021, 47 Degrees

---
Samir Kahvedzic

GitHub Notification Service for Slack
This service aims to send messages to a users Slack account, composed of Github repository events. 
A user may subscribe to the Github repositories for which they would like to receive events from.
Slack commands are used to subscribe to events, or alter parameters of the service for the user.

Architecture

- Subscriber Service

- Event Service

- Notification Service

- Slack bot: slack integration able to interact with the Notification system

---

Stack: Typelevel

Libraries: Circe, Http4s, Cats-Effect

---

Project Spec: [Google Drive Project Spec](https://docs.google.com/document/d/1XnYEOn5uki0VMVOfcrYNhl-3uaruZBJrHM5zsnQuID0/edit#heading=h.piwgc9cf2cox)