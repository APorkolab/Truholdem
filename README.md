# Truholdem

Truholdem is a sophisticated poker program designed to offer a comprehensive platform for learning, practicing, and simulating Texas Hold'em poker games. This README provides an in-depth overview of the project, installation instructions, usage guidelines, future plans, and contribution details.

## Project Overview

Truholdem is developed to simulate Texas Hold'em poker, providing a rich environment for users to enhance their poker skills, test strategies, and enjoy engaging poker sessions. The project features a robust backend in Java and an interactive frontend using Angular and SCSS.
The main goal was the imitate the worlds of the Win95/Win98 poker simulators and get an overwhelmingly positive experience for the users.

### Features

-   **Game Simulation**: Fully functional Texas Hold'em poker simulation, allowing users to experience realistic gameplay.
-   **User Interface**: Intuitive and responsive interface designed with modern web technologies.
-   **Multiplayer Support**: Capability for multiple players to join and play poker games.
-   **Game History**: Track and review past games to analyze strategies and improve skills.
-   **AI Opponents**: Play against AI opponents of varying difficulty levels to refine your techniques.
-   **Customizable Settings**: Adjust game rules and settings to create personalized poker experiences.
-   **Cross-Platform Compatibility**: Accessible on various devices and platforms, ensuring a seamless experience.

## Installation

To get started with Truholdem, follow these steps:

### Prerequisites

-   Java Development Kit (JDK) installed - the project uses Java 17.
-   Node.js and npm installed.

### Steps

1.  Clone the repository: 
					
		git clone https://github.com/APorkolab/Truholdem.git
    
2.  Navigate to the project directory:

		cd Truholdem
    
3.  Install backend dependencies and build the project: 

		cd backend ./mvn clean install
    
4.  Install frontend dependencies:

		cd ../frontend npm install
    
5.  Start the backend server.
    
6.  Start the frontend development server: +++ npm start +++
    

## Usage

Once the servers are running, access the application in your web browser at `http://localhost:3000`. Use the interface to play simulated Texas Hold'em poker games, practice strategies, and enjoy learning poker.

## Future Plans/Knowing bugs:

Truholdem has ambitious plans for future enhancements:

-   **All-in and Reset game functionality**: These features have been done, but the user testing was not perfect.
- **Fixing errors**: When a new game is played, the above card is randomly slipped to the left onto the other player's cards, which can be "put in place" by reloading.
-   **Mobile App Development**: Expanding the platform to mobile devices for on-the-go poker practice.
-   **Advanced AI**: Improving AI algorithms for more challenging and realistic gameplay.
-   **Tournament Mode**: Introducing tournament-style gameplay with leaderboards and rewards.
-   **Social Features**: Adding features for social interaction, such as chat and friend lists.
-   **Additional Poker Variants**: Incorporating other popular poker variants like Omaha and Seven-Card Stud.
-   **Comprehensive Tutorials**: Develop detailed tutorials and guides to help new players learn the game.

## Contributing

Contributions are welcome! If you'd like to contribute to Truholdem, please follow these guidelines:

1.  Fork the repository.
    
2.  Create a new branch for your feature or bugfix: 

	     git checkout -b feature-name
    
3.  Commit your changes: +++ 

		git commit -m "Description of feature or fix"
    
4.  Push to the branch:

		git push origin feature-name
    
5.  Open a pull request with a description of your changes.
    

## License

This project is licensed under the MIT License.

## Author

Truholdem is developed and maintained by [Adam Porkolab](https://github.com/APorkolab). For any questions or inquiries, please open an issue on the repository or contact the author directly.
