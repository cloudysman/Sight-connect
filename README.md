# Sight-Connect

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![Version](https://img.shields.io/badge/version-1.0.0-blue)

Sight-Connect is a real-time navigation tool that uses sensor technology and visual input to guide users in both indoor and outdoor environments.

## Features
- Real-time, step-by-step navigation
- Indoor and outdoor support
- Customizable settings for user preferences

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/cloudysman/Sight-connect.git
   ```
2. Navigate to the project directory:
  ```bash
  cd Sight-connect
  ```
## Usage Example
Here’s a simple example of how to start using Sight-Connect after installation:
  ```bash
from sight_connect import Navigator

# Initialize the navigator with custom settings
navigator = Navigator(indoor=True, outdoor=True)

# Set starting point and destination
navigator.set_route(start="Main Entrance", destination="Room 303")

# Begin real-time navigation
navigator.start_navigation()
   ```
## License

This project is licensed under the MIT License. For more details, see the [LICENSE](./LICENSE.md) file.
