# traffic_data

Requirements

1. A webserver with Java 8 and Maven installed
2. A Google Maps API key - https://developers.google.com/maps/documentation/javascript/get-api-key

Install/Config

1. Clone the repository to the folder of your choice on your webserver
2. Build the jar - mvn package
3. Move/copy the jar from the target directory up to the root directory of the project
4. Update run.sh with appropriate values for <home address> and <work address> in the following format: "123 Fake Street Sometown CA 01234" (no commas or other punctuation, everything separated by spaces)
5. Update run.sh with your Google Maps API key
6. Make run.sh executable (if necessary) - chmod +x run.sh
7. Execute run.sh once manually to make sure everything is working
8. Add an entry in your crontab to run the script every 5 minutes. EX: */5 * * * * /var/www/html/traffic_data/run.sh
9. You should now be able to navigate to the appropriate URL in your browser and see charts for both drives - Home to Work and Work to Home  