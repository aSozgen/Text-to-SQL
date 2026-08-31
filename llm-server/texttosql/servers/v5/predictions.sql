-- Soru 1: What are the address details and biographical information of each student?
SELECT T1.student_details , T1.bio_data FROM Students AS T1 JOIN Transcripts AS T2 ON T1.student_id = T2.student_id

-- Soru 2: What is the product average price?
SELECT AVG(product_price) AS average_price FROM Products

-- Soru 3: Count the number of different plane names across all pilots.
SELECT count(DISTINCT plane_name) FROM PilotSkills

-- Soru 4: How many reference papers does paper with id D12-1027 have?
SELECT COUNT(*) AS reference_count FROM Citations WHERE paper_id = 'D12-1027'

-- Soru 5: What is the maximum rating for sailors?
SELECT max(rating) FROM sailors

-- Soru 6: What is the average sale price of books written by George Orwell?
SELECT AVG(Book.SalePrice) FROM Author JOIN Author_Book ON Author.idAuthor = Author_Book.Author JOIN Book ON Author_Book.ISBN = Book.ISBN WHERE Author.Name = 'George Orwell'

-- Soru 7: What is the average age for customers with a membership credit above the average?
SELECT avg(age) FROM Customers WHERE membership_credit > (SELECT avg(membership_credit) FROM Customers)

-- Soru 8: What is the list of channel details ordered alphabetically ?
SELECT Channel_Details FROM CHANNELS ORDER BY Channel_Details

-- Soru 9: What are the codes of boxes for which the value is greater than the value of any box that contains Rocks?
SELECT Code FROM Boxes WHERE Value > (SELECT MAX(Value) FROM Boxes WHERE Contents = 'Rocks')

-- Soru 10: Find the number of channels that do not run any program.
SELECT COUNT(*) FROM channel c WHERE NOT EXISTS ( SELECT 1 FROM program p WHERE p.Channel_ID = c.Channel_ID )

-- Soru 11: How many channels have the word 'bbc' in their internet link?
SELECT COUNT(*) FROM channel WHERE Internet LIKE '%bbc%'

-- Soru 12: Find all the details of the customers who have been involved in an interaction with status `` Stuck '' and service and channel detail `` bad '' .
SELECT C.Customer_Details FROM Customers AS C JOIN Interactions AS I ON C.Customer_ID = I.Customer_ID JOIN Services AS S ON I.Service_ID = S.Service_ID JOIN Channels AS CH ON I.Channel_ID = CH.Channel_ID WHERE I.Status = 'Stuck' AND S.Service_Details = 'bad' AND CH.Channel_Details = 'bad'

-- Soru 13: Find the number of reservations for each boat with id greater than 50.
SELECT count(*) , T1.name FROM Boats AS T1 JOIN Reserves AS T2 ON T1.bid = T2.bid WHERE T1.bid > 50 GROUP BY T1.name

-- Soru 14: Which store has the headphones in stock? Give me the store name and the total quantity.
SELECT s.Name, SUM(st.Quantity) AS Total_Quantity FROM store s JOIN stock st ON s.Store_ID = st.Store_ID JOIN headphone h ON st.Headphone_ID = h.Headphone_ID GROUP BY s.Name

-- Soru 15: Show agency ids and details with at least 2 clients.
SELECT a.agency_id, a.agency_details FROM Agencies a JOIN Clients c ON a.agency_id = c.agency_id GROUP BY a.agency_id, a.agency_details HAVING COUNT(c.client_id) >= 2

-- Soru 16: List the order date of the orders who are placed by customers with at least 2 payment methods.
SELECT o.order_date FROM Orders o JOIN Payments p ON o.order_id = p.order_id GROUP BY o.customer_id HAVING COUNT(DISTINCT p.payment_method) >= 2

-- Soru 17: What is the distance between Boston and Newark?
SELECT distance FROM Direct_distance WHERE (city1_code = 'BOS' AND city2_code = 'NWK') OR (city1_code = 'NWK' AND city2_code = 'BOS')

-- Soru 18: What are the locations of warehouses in which boxes that contain Rocks and Scissors are kept?
SELECT T2.Location FROM Boxes AS T1 JOIN Warehouses AS T2 ON T1.Warehouse = T2.Code WHERE T1.Contents = 'Rocks' INTERSECT SELECT T2.Location FROM Boxes AS T1 JOIN Warehouses AS T2 ON T1.Warehouse = T2.Code WHERE T1.Contents = 'Scissors'

-- Soru 19: What are the rating and average age for sailors who reserved red boats for each rating?
SELECT T1.rating , avg(T2.age) FROM Reserves AS T3 JOIN Boats AS T1 ON T3.bid = T1.bid JOIN Sailors AS T2 ON T3.sid = T2.sid WHERE T1.color = 'red' GROUP BY T1.rating

-- Soru 20: Find the number of movies whose rating is not null.
SELECT count(*) FROM Movies WHERE rating != "null"

-- Soru 21: How many pilots who are older than 40 or younger than 30?
SELECT count(*) FROM PilotSkills WHERE age > 40 OR age < 30

-- Soru 22: Please show the most common manufacturer of clubs.
SELECT Manufacturer, COUNT(*) as Count FROM club GROUP BY Manufacturer ORDER BY Count DESC LIMIT 1

-- Soru 23: Show the names of club leaders and the names of clubs they joined.
SELECT m.Name AS Club_Leader, c.Club_Name FROM club_leader cl JOIN member m ON cl.Member_ID = m.Member_ID JOIN club c ON cl.Club_ID = c.Club_ID

-- Soru 24: Find the total value of boxes stored in the warehouse with the largest capacity.
SELECT SUM(T2.Value) FROM Warehouses AS T1 JOIN Boxes AS T2 ON T1.Code = T2.Warehouse WHERE T1.Capacity = (SELECT MAX(Capacity) FROM Warehouses)

-- Soru 25: What are the names of languages that are not the official language of any country?
SELECT name FROM languages WHERE id NOT IN (SELECT language_id FROM official_languages)

-- Soru 26: What are the different plane names, ordered alphabetically?
SELECT DISTINCT plane_name FROM Hangar ORDER BY plane_name

-- Soru 27: Show each customer name and the total quantities of dishes ordered by that customer.
SELECT c.Name, SUM(co.Quantity) AS Total_Quantity FROM customer c JOIN customer_order co ON c.Customer_ID = co.Customer_ID GROUP BY c.Customer_ID

-- Soru 28: what is the name and id of every sailor who has a rating greater than 2 and reserved a boat.
SELECT T1.name , T1.sid FROM sailors AS T1 JOIN reserves AS T2 ON T1.sid = T2.sid WHERE T1.rating > 2

-- Soru 29: Show the official languages and the number of countries speaking each language.
SELECT l.name AS language_name, COUNT(ol.country_id) AS number_of_countries FROM languages l JOIN official_languages ol ON l.id = ol.language_id GROUP BY l.name

-- Soru 30: What is the name and age of the customer with the most membership credit?
SELECT name , age FROM Customers ORDER BY membership_credit DESC LIMIT 1

-- Soru 31: Find the names of all the services which both have been used by customer "Hardy Kutch" and have been rated "bad" in one of the customer interactions.
SELECT DISTINCT s.Service_Details FROM Services s JOIN Customers_and_Services cas ON s.Service_ID = cas.Service_ID JOIN Customers c ON cas.Customer_ID = c.Customer_ID JOIN Customer_Interaction_Ratings cir ON cas.Customers_and_Services_ID = cir.Customers_and_Services_ID WHERE c.Customer_Details = 'Hardy Kutch' AND cir.Rating = 'bad'

-- Soru 32: What is the cheapest good with cheese flavor?
SELECT T1.Id FROM goods AS T1 JOIN items AS T2 ON T1.Id = T2.Item WHERE T1.Flavor = "cheese" ORDER BY T1.Price LIMIT 1

-- Soru 33: Count the number of vehicles.
SELECT COUNT(*) FROM Vehicles

-- Soru 34: How many bikes are heavier than 780 grams?
SELECT count(*) FROM bike WHERE weight > 780

-- Soru 35: How many properties have 1 parking lot or 1 garage?
SELECT COUNT(*) FROM Properties WHERE has_parking_lot = 1 OR has_garage = 1

-- Soru 36: Show all staff names who have been both speaker and sponsor in some conference.
SELECT s.name FROM staff s WHERE s.staff_ID IN (SELECT sp.staff_ID FROM speakers sp) AND s.staff_ID IN (SELECT sn.staff_ID FROM sponsors sn)

-- Soru 37: What are the models which have not been driven by any drivers?
SELECT v.Model FROM vehicle v LEFT JOIN vehicle_driver vd ON v.Vehicle_ID = vd.Vehicle_ID WHERE vd.Driver_ID IS NULL

-- Soru 38: Who sent most number of packages? List client name and number of packages sent by that client.
SELECT e.Name AS ClientName, COUNT(s.ShipmentID) AS NumberOfPackagesSent FROM Employee e JOIN Shipment s ON e.EmployeeID = s.Manager GROUP BY e.EmployeeID, e.Name ORDER BY NumberOfPackagesSent DESC LIMIT 1

-- Soru 39: List the start time and the end time of the students' addresses for the students who have 2 transcripts.
SELECT s.student_id, MIN(t.date_of_transcript) AS start_time, MAX(t.date_of_transcript) AS end_time FROM Students s JOIN Transcripts t ON s.student_id = t.student_id GROUP BY s.student_id HAVING COUNT(t.transcript_id) = 2

-- Soru 40: Give the ids of Cookies or Cakes that cost between 3 and 7 dollars.
SELECT id FROM goods WHERE food IN ('Cookie','Cake') AND price BETWEEN 3 AND 7

-- Soru 41: Among the buildings not completed in 1980, what is the maximum number of stories?
SELECT max(Number_of_Stories) FROM building WHERE Completed_Year != 1980

-- Soru 42: What is the title of the sculpture that was created in the most recent year ?
SELECT title FROM Sculptures ORDER BY year DESC LIMIT 1

-- Soru 43: How many cyclists did not purchase any bike after year 2015?
SELECT count(*) FROM cyclist WHERE id NOT IN ( SELECT cyclist_id FROM cyclists_own_bikes WHERE purchase_year > 2015 )

-- Soru 44: Show the first name and the last name for students living in state MD.
SELECT Fname , LName FROM Student WHERE city_code IN (SELECT city_code FROM City WHERE state = 'MD')

-- Soru 45: Give the different payment method codes that customers use.
SELECT DISTINCT payment_method_code FROM Customers

-- Soru 46: How many drivers have been racing in each racing series?
SELECT Racing_Series, COUNT(Driver_ID) AS Number_of_Drivers FROM driver GROUP BY Racing_Series

-- Soru 47: What are the model, class, and construction of the cheapest headphone?
SELECT Model, Class, Construction FROM headphone ORDER BY Price ASC LIMIT 1

-- Soru 48: Find the names of districts whose government links use a 'gov' domain.
SELECT Name FROM district WHERE Government_website LIKE '%.gov'

-- Soru 49: List the names of regions in alphabetical order.
SELECT name FROM region ORDER BY name

-- Soru 50: Find the name of stores which have no headphone in stock.
SELECT s.Name FROM store s LEFT JOIN stock st ON s.Store_ID = st.Store_ID AND st.Headphone_ID IS NULL

-- Soru 51: Show the type of powertrain with at least two vehicles, and the average annual fuel cost for vehicles in each such type.
SELECT Type_of_powertrain, AVG(Annual_fuel_cost) as Average_Annual_Fuel_Cost FROM Vehicles GROUP BY Type_of_powertrain HAVING COUNT(*) >= 2

-- Soru 52: What is the invoice id, status code, and details for the invoice with most number of payments.
SELECT i.invoice_id, i.invoice_status, i.invoice_details FROM Invoices i JOIN Payments p ON i.invoice_id = p.invoice_id GROUP BY i.invoice_id, i.invoice_status, i.invoice_details ORDER BY COUNT(p.payment_id) DESC LIMIT 1

-- Soru 53: What is the agent id and details for the agency with most staff?
SELECT a.agency_id, a.agency_details FROM Agencies a JOIN Staff s ON a.agency_id = s.agency_id GROUP BY a.agency_id, a.agency_details ORDER BY COUNT(s.staff_id) DESC LIMIT 1

-- Soru 54: What is the total weight of all the packages that customer Leo Wong sent?
SELECT SUM(Packages.Weight) FROM Packages JOIN Orders ON Packages.OrderID = Orders.OrderID JOIN Customers ON Orders.CustomerID = Customers.CustomerID WHERE Customers.Name = 'Leo Wong'

-- Soru 55: Give the ids of goods that cost less than 3 dollars.
SELECT id FROM goods WHERE price < 3

-- Soru 56: What are the document subset names?
SELECT document_subset_name FROM document_subsets

-- Soru 57: What is the receipt number and date corresponding to the receipt for which the most expensive item was purchased?
SELECT T1.Receipt , T1.Date FROM receipts AS T1 JOIN items AS T2 ON T1.Receipt = T2.Receipt WHERE T2.Item IN (SELECT Id FROM goods ORDER BY Price DESC LIMIT 1)

-- Soru 58: Return the date stamp and property name for each property history event, sorted by date stamp.
SELECT date_stamp, property_name FROM Property_History ORDER BY date_stamp

-- Soru 59: What is the name and id of every sailor who reserved one or more boats?
SELECT T1.name , T1.sid FROM sailors AS T1 JOIN reserves AS T2 ON T1.sid = T2.sid GROUP BY T1.sid HAVING count(*) >= 1

-- Soru 60: Find the name and nationality of the people who did not participate in any ACL conference.
SELECT name, Nationality FROM staff WHERE staff_ID NOT IN (SELECT staff_ID FROM conference_participation WHERE Conference_ID = (SELECT Conference_ID FROM conference WHERE Conference_Name = 'ACL'))

-- Soru 61: What were the ids, dates, and status codes for orders made by Jeromy?
SELECT t1.order_id , t1.order_date , t1.status_code FROM customer_orders AS t1 JOIN customers AS t2 ON t1.customer_id = t2.customer_id WHERE t2.customer_name = "Jeromy"

-- Soru 62: What are the collection subset ids, names, and number of collections for each subset?
SELECT Collection_Subset_ID, Collection_Subset_Name, COUNT(*) AS Number_of_Collections FROM Collection_Subsets GROUP BY Collection_Subset_ID, Collection_Subset_Name

-- Soru 63: Count the number of papers which cited a paper with id A00-1002.
SELECT COUNT(*) FROM Citation WHERE cited_paper_id = 'A00-1002'

-- Soru 64: List document id of all documents.
SELECT Document_Object_ID FROM Document_Objects

-- Soru 65: List the dates of all shipments.
SELECT Date FROM Shipment

-- Soru 66: What are the average prices of goods with blackberry or blueberry flavor?
SELECT AVG(Price) FROM goods WHERE Flavor = "blackberry" OR Flavor = "blueberry"

-- Soru 67: Where is the plane F-14 Fighter located?
SELECT T2.location FROM Hangar AS T1 JOIN Plane AS T2 ON T1.plane_name = T2.plane_name WHERE T2.plane_name = "F-14 Fighter"

-- Soru 68: Find the number of reservations by sailors with id greater than 1 for each boat.
SELECT count(*) , T1.name FROM boats AS T1 JOIN reserves AS T2 ON T1.bid = T2.bid WHERE T2.sid > 1 GROUP BY T1.name

-- Soru 69: List all names of drivers in descending alphabetical order.
SELECT Driver_Name FROM driver ORDER BY Driver_Name DESC

-- Soru 70: Show the types of questions that have at least three questions.
SELECT Type_of_Question_Code FROM Questions GROUP BY Type_of_Question_Code HAVING COUNT(*) >= 3

-- Soru 71: Find the headphone classes that contain both headphones using "Bowls" earpads and headphones using "Comfort Pads" earpads.
SELECT Class FROM headphone WHERE Earpads = 'Bowls' INTERSECT SELECT Class FROM headphone WHERE Earpads = 'Comfort Pads'

-- Soru 72: What is the name of the director who is in the "Dracula" program?
SELECT T2.Name FROM program AS T1 JOIN director AS T2 ON T1.Director_ID = T2.Director_ID WHERE T1.Title = 'Dracula'

-- Soru 73: Find the first names and number of works of all artists who have at least two paintings?
SELECT a.fname, COUNT(p.paintingID) AS num_paintings FROM Artists a JOIN Paintings p ON a.artistID = p.painterID GROUP BY a.artistID HAVING COUNT(p.paintingID) >= 2

-- Soru 74: List the names of institutions in ascending alphabetical order.
SELECT Name FROM institution ORDER BY Name ASC

-- Soru 75: What are the names and locations of the universities that did not have any staff participating in any conferences in 2004?
SELECT DISTINCT T1.institution_name, T1.location FROM institution AS T1 LEFT JOIN staff AS T2 ON T1.institution_id = T2.institution_id LEFT JOIN conference_participation AS T3 ON T2.staff_id = T3.staff_id WHERE T3.conference_year != 2004

-- Soru 76: Show all customer ids and customer names.
SELECT customer_id , customer_name FROM Customers

-- Soru 77: What are the names and ids of sailors who reserved red or blue boats?
SELECT T1.name , T1.sid FROM sailors AS T1 JOIN reserves AS T2 ON T1.sid = T2.sid JOIN boats AS T3 ON T2.bid = T3.bid WHERE color = 'red' OR color = 'blue'

-- Soru 78: What are the maximum height and width of paintings for each year?
SELECT year, MAX(height_mm) AS max_height, MAX(width_mm) AS max_width FROM Paintings GROUP BY year

-- Soru 79: What are the ids of sailors who have not reserved a boat?
SELECT sid FROM sailors EXCEPT SELECT sid FROM reserves

-- Soru 80: List the codes and descriptions for all staff roles.
SELECT staff_role_code , staff_role_description FROM Ref_Staff_Roles

-- Soru 81: Find all the countries where some drivers have points above 150.
SELECT DISTINCT c.Country FROM driver d JOIN country c ON d.Country = c.Country_Id WHERE d.Points > 150

-- Soru 82: List package number and weight of top 3 lightest packages.
SELECT PackageID, Weight FROM Package ORDER BY Weight ASC LIMIT 3

-- Soru 83: Show ids for orders including both "Pride and Prejudice" and "The Little Prince".
SELECT o.IdOrder FROM Orders o JOIN Order_Details od ON o.IdOrder = od.IdOrder JOIN Book b ON od.ISBN = b.ISBN WHERE b.Title = 'Pride and Prejudice' INTERSECT SELECT o.IdOrder FROM Orders o JOIN Order_Details od ON o.IdOrder = od.IdOrder JOIN Book b ON od.ISBN = b.ISBN WHERE b.Title = 'The Little Prince'

-- Soru 84: What are the names of the staff members who have been both a speaker and a sponsor at some conference?
SELECT T1.name FROM staff AS T1 JOIN conference_participation AS T2 ON T1.staff_id = T2.staff_id WHERE T2.role = 'speaker' INTERSECT SELECT T1.name FROM staff AS T1 JOIN conference_participation AS T2 ON T1.staff_id = T2.staff_id WHERE T2.role = 'sponsor'

-- Soru 85: What are the ids and details of the staff who have attended at least 1 meetings and have the detail with letter 's'?
SELECT T2.staff_id, T2.staff_details FROM Meetings AS T1 JOIN Staff AS T2 ON T1.staff_id = T2.staff_id GROUP BY T2.staff_id HAVING COUNT(*) >= 1 AND T2.staff_details LIKE '%s%'

-- Soru 86: What are the 10 most cited papers, and how many citations did each have?
SELECT p.paper_id, COUNT(*) as citation_count FROM Paper p JOIN Citation c ON p.paper_id = c.cited_paper_id GROUP BY p.paper_id ORDER BY citation_count DESC LIMIT 10

-- Soru 87: What is the name of the institution with no staff in the records?
SELECT Institution_Name FROM institution WHERE Institution_ID NOT IN (SELECT Institution_ID FROM staff)

-- Soru 88: How many different types of contents are stored in each warehouse?
SELECT count(DISTINCT contents) , T1.code FROM warehouses AS T1 JOIN boxes AS T2 ON T1.code = T2.warehouse GROUP BY T1.code

-- Soru 89: Find the countries where no driver come from.
SELECT Country FROM country WHERE Country_Id NOT IN (SELECT Country FROM driver)

-- Soru 90: What are the names of distinct racing bikes that are purchased by the cyclists with better results than '4:21.558' ?
SELECT DISTINCT T1.product_name FROM bike AS T1 JOIN cyclists_own_bikes AS T2 ON T1.id = T2.bike_id JOIN cyclist AS T3 ON T3.id = T2.cyclist_id WHERE T3.result < '4:21.558'

-- Soru 91: How many reservations exist for each boat that has more than 1 reservation already?
SELECT count(*) , T1.name FROM Boats AS T1 JOIN Reserves AS T2 ON T1.bid = T2.bid GROUP BY T1.name HAVING count(*) > 1

-- Soru 92: What are the titles of books with sale prices above the average sale price across all books?
SELECT Title FROM Book WHERE SalePrice > ( SELECT avg(SalePrice) FROM Book )

-- Soru 93: Show the number of documents.
SELECT COUNT(staff_id) AS number_of_documents FROM Staff

-- Soru 94: Which customers used the least commonly-used service ? Give me the distinct customer details .
SELECT DISTINCT c.Customer_Details FROM Customers c JOIN Customers_and_Services cas ON c.Customer_ID = cas.Customer_ID JOIN Services s ON cas.Service_ID = s.Service_ID GROUP BY c.Customer_ID ORDER BY COUNT(s.Service_ID) ASC LIMIT 1

-- Soru 95: Count the number of drivers who have not driven any vehicles.
SELECT COUNT(*) FROM driver WHERE Driver_ID NOT IN (SELECT Driver_ID FROM vehicle_driver)

-- Soru 96: List the detail and id of the teacher who teaches the most courses.
SELECT t.teacher_id, t.teacher_details, COUNT(c.course_id) AS course_count FROM Teachers t JOIN Courses c ON t.teacher_id = c.teacher_id GROUP BY t.teacher_id, t.teacher_details ORDER BY course_count DESC LIMIT 1

-- Soru 97: Show different nationalities of customers, along with the number of customers of each nationality.
SELECT Nationality, COUNT(*) AS Number_of_Customers FROM customer GROUP BY Nationality

-- Soru 98: Show the names of club leaders that joined their club before 2018.
SELECT T3.Name FROM club AS T1 JOIN club_leader AS T2 ON T1.Club_ID = T2.Club_ID JOIN member AS T3 ON T2.Member_ID = T3.Member_ID WHERE T2.Year_Join < 2018

-- Soru 99: Find the code and content of all boxes whose value is higher than the value of all boxes with Scissors as content.
SELECT Code , Contents FROM Boxes WHERE Value > ( SELECT MAX(Value) FROM Boxes WHERE Contents = 'Scissors' )

-- Soru 100: What are the names of all Hardware products, sorted by price ascending?
SELECT product_name FROM products WHERE product_type_code = 'Hardware' ORDER BY product_price ASC

-- Soru 101: What are the full names of students living in MD?
SELECT Fname , Lname FROM STUDENT WHERE city_code = "MD"

-- Soru 102: List all states with at least two cities.
SELECT s.state_name, COUNT(c.city_code) AS city_count FROM City c JOIN State s ON c.state_code = s.state_code GROUP BY s.state_name HAVING COUNT(c.city_code) >= 2

-- Soru 103: What are the first and last names of the artist who lived the longest?
SELECT fname, lname FROM Artists ORDER BY (deathYear - birthYear) DESC LIMIT 1

-- Soru 104: How many staff do we have?
SELECT COUNT(*) FROM Staff

-- Soru 105: Find the headphone model whose total quantity in stock is the largest.
SELECT h.Model, SUM(s.Quantity) AS Total_Quantity FROM stock s JOIN headphone h ON s.Headphone_ID = h.Headphone_ID GROUP BY h.Model ORDER BY Total_Quantity DESC LIMIT 1

-- Soru 106: Show all the different invoice ids and statuses of the payments
SELECT DISTINCT invoice_id, invoice_status FROM Invoices

-- Soru 107: Give me the detention start date for all the detention records.
SELECT date_of_transcript FROM Transcripts WHERE transcript_details LIKE '%detention%'

-- Soru 108: How many authors are of age above 30 for each gender?
SELECT Gender, COUNT(*) AS Number_of_Authors FROM author WHERE Age > 30 GROUP BY Gender

-- Soru 109: List all pilot names sorted by their ages in the descending order.
SELECT pilot_name FROM PilotSkills ORDER BY age DESC

-- Soru 110: What is the total number of universities located in Illinois or Ohio?
SELECT COUNT(*) FROM university WHERE State IN ('Illinois', 'Ohio')

-- Soru 111: What is the number of employees that do not have clearance on Mars ?
SELECT COUNT(*) FROM Employee e WHERE NOT EXISTS ( SELECT 1 FROM Has_Clearance hc JOIN Planet p ON hc.Planet = p.PlanetID WHERE hc.Employee = e.EmployeeID AND p.Name = 'Mars' )

-- Soru 112: What are the names of clubs, ordered descending by the average earnings of players within each?
SELECT T1.Name FROM CLub AS T1 JOIN player AS T2 ON T1.Club_ID = T2.Club_ID GROUP BY T1.Name ORDER BY avg(T2.Earnings) DESC

-- Soru 113: Return the id and detail for the agency with the most staff.
SELECT a.agency_id, a.agency_details FROM Agencies a JOIN Staff s ON a.agency_id = s.agency_id GROUP BY a.agency_id, a.agency_details ORDER BY COUNT(s.staff_id) DESC LIMIT 1

-- Soru 114: What are the distinct ids and product names of the bikes that are purchased after year 2015?
SELECT DISTINCT T1.id , T1.product_name FROM bike AS T1 JOIN cyclists_own_bikes AS T2 ON T1.id = T2.bike_id JOIN cyclist AS T3 ON T2.cyclist_id = T3.id WHERE T2.purchase_year > 2015

-- Soru 115: Count the number of addresses.
SELECT COUNT(address_id) AS total_addresses FROM Addresses

-- Soru 116: Show the names of languages that are the official language for both countries with overall score greater than 95 and countries with overall score less than than 90.
SELECT l.name FROM languages l JOIN official_languages ol ON l.id = ol.language_id JOIN ( SELECT country_id FROM countries WHERE overall_score > 95 ) c1 ON ol.country_id = c1.country_id JOIN ( SELECT country_id FROM countries WHERE overall_score < 90 ) c2 ON ol.country_id = c2.country_id

-- Soru 117: How many movies had a 'G' rating?
SELECT count(*) FROM Movies WHERE Rating = 'G'

-- Soru 118: What are the names of pilots whose age is below the average age, ordered by age?
SELECT pilot_name FROM pilotskills WHERE age < ( SELECT avg(age) FROM pilotskills ) ORDER BY age

-- Soru 119: Which good has "70" in its id? And what is its price?
SELECT id , price FROM goods WHERE id LIKE '%70%'

-- Soru 120: What are the names of the different clients who have made an order?
SELECT DISTINCT T1.Name FROM Client AS T1 JOIN Orders AS T2 ON T1.IdClient = T2.IdClient

-- Soru 121: Give the ids of goods that are more than twice as expensive as the average good.
SELECT Id FROM goods WHERE Price > ( SELECT avg(Price) * 2 FROM goods )

-- Soru 122: Find the locations that have paintings before 1885 and no work with medium on canvas?
SELECT location FROM Paintings WHERE year < 1885 EXCEPT SELECT location FROM Paintings WHERE mediumOn = 'canvas'

-- Soru 123: What is the number of child collections belonging to the collection named Best?
SELECT COUNT(*) FROM Collections c1 JOIN Collections c2 ON c1.Collection_ID = c2.Parent_Collection_ID WHERE c1.Collection_Name = 'Best'

-- Soru 124: Which student answer texts were given both "Normal" and "Absent" as comments?
SELECT sa1.Answer_ID, sa1.Student_ID, sa1.Question_ID, sa1.Exam_ID, sa1.Comment FROM Student_Answers sa1 JOIN ( SELECT Student_ID, Question_ID, Exam_ID FROM Student_Answers WHERE Comment = 'Normal' ) AS normal_comments ON sa1.Student_ID = normal_comments.Student_ID AND sa1.Question_ID = normal_comments.Question_ID AND sa1.Exam_ID = normal_comments.Exam_ID JOIN ( SELECT Student_ID, Question_ID, Exam_ID FROM Student_Answers WHERE Comment = 'Absent' ) AS absent_comments ON sa1.Student_ID = absent_comments.Student_ID AND sa1.Question_ID = absent_comments.Question_ID AND sa1.Exam_ID = absent_comments.Exam_ID

-- Soru 125: What is the rating of the book with the largest number of chapters?
SELECT T2.Rating FROM book AS T1 JOIN review AS T2 ON T1.Book_ID = T2.Book_ID ORDER BY T1.Chapters DESC LIMIT 1

-- Soru 126: What is the count of the sailors whose name starts with letter D ?
SELECT COUNT(*) FROM sailors WHERE name LIKE 'D%'

-- Soru 127: What is the average population for all regions?
SELECT AVG(Population) AS Average_Population FROM region

-- Soru 128: how many different earpads are there?
SELECT COUNT(DISTINCT Earpads) AS Number_of_Different_Earpads FROM headphone

-- Soru 129: List the name of clubs whose manufacturer is not "Nike"
SELECT Name FROM club WHERE Manufacturer != "Nike"

-- Soru 130: What is the average number of units sold in millions of games played by players with position "Guard"?
SELECT AVG(g.Units_sold_Millions) FROM game g JOIN player_stats ps ON g.Game_ID = ps.Game_ID WHERE ps.Player_position = 'Guard'

-- Soru 131: What are the id and name of the cyclist who owns the most bikes?
SELECT T1.id , T1.name FROM cyclist AS T1 JOIN cyclists_own_bikes AS T2 ON T1.id = T2.cyclist_id JOIN bike AS T3 ON T2.bike_id = T3.id GROUP BY T1.id ORDER BY count(*) DESC LIMIT 1

-- Soru 132: List all university names in ascending order of their reputation points.
SELECT T1.University_Name FROM university AS T1 JOIN overall_ranking AS T2 ON T1.University_ID = T2.University_ID ORDER BY T2.Reputation_point ASC

-- Soru 133: What are the maximum and minimum weight of all bikes?
SELECT max(weight) , min(weight) FROM bike

-- Soru 134: Show the book title corresponding to the book with the most number of orders.
SELECT b.Title FROM Book b JOIN Orders o ON b.ISBN = o.IdOrder GROUP BY b.ISBN ORDER BY COUNT(o.IdOrder) DESC LIMIT 1

-- Soru 135: Sort all the books in descending order of release date, and return the book titles.
SELECT Title FROM book ORDER BY Release_date DESC

-- Soru 136: How many papers does Columbia University have in or before 2009 ?
SELECT COUNT(DISTINCT p.paper_id) AS paper_count FROM Paper p JOIN Author_list al ON p.paper_id = al.paper_id JOIN Affiliation a ON al.affiliation_id = a.affiliation_id WHERE a.name = 'Columbia University' AND p.year <= 2009

-- Soru 137: What are the minimum and maximum prices of food goods, ordered by food?
SELECT min(price) , max(price) FROM goods ORDER BY food

-- Soru 138: Find the details of the customer who has used services the most times.
SELECT C.Customer_Details FROM Customers C JOIN ( SELECT Customer_ID FROM Customers_and_Services GROUP BY Customer_ID ORDER BY COUNT(*) DESC LIMIT 1 ) AS Most_Frequent_Customer ON C.Customer_ID = Most_Frequent_Customer.Customer_ID

-- Soru 139: Find the name of the affiliation whose address contains 'China' and publishes the greatest number of papers.
SELECT a.name FROM Affiliation a JOIN Author_list al ON a.affiliation_id = al.affiliation_id WHERE a.address LIKE '%China%' GROUP BY a.name ORDER BY COUNT(al.paper_id) DESC LIMIT 1

-- Soru 140: List all package numbers received by Leo Wong ?
SELECT s.ShipmentID FROM Employee e JOIN Has_Clearance hc ON e.EmployeeID = hc.Employee JOIN Shipment s ON hc.Shipment = s.ShipmentID WHERE e.Name = 'Leo Wong'

-- Soru 141: What is the most common valid answer text?
SELECT Answer_Text, COUNT(*) AS Frequency FROM Answers GROUP BY Answer_Text ORDER BY Frequency DESC LIMIT 1

-- Soru 142: What is the name, city, and state of the university with number 1 ranked Accounting major?
SELECT u.University_Name, u.City, u.State FROM university u JOIN overall_ranking o ON u.University_ID = o.University_ID JOIN major m ON u.University_ID = m.University_ID AND m.Major_Name = 'Accounting' WHERE o.Rank = 1

-- Soru 143: Which locations have paintings in the mediums of on panel and on canvas?
SELECT location FROM Paintings WHERE mediumOn = 'on panel' OR mediumOn = 'on canvas'

-- Soru 144: Show the types of books that have both books with more than 75 chapters and books with less than 50 chapters.
SELECT TYPE FROM book WHERE Chapters > 75 INTERSECT SELECT TYPE FROM book WHERE Chapters < 50

-- Soru 145: Find the unique contents of all boxes whose value is higher than the average value of all boxes.
SELECT DISTINCT Contents FROM Boxes WHERE Value > ( SELECT AVG(Value) FROM Boxes )

-- Soru 146: List the year, location and title of paintings whose height is longer than 1000 ordered by title.
SELECT year , location , title FROM Paintings WHERE height_mm > 1000 ORDER BY title

-- Soru 147: For each press, return its name and the number of books that have sale amount above 1000.
SELECT p.Name, COUNT(b.Book_ID) AS Number_of_Books FROM press p JOIN book b ON p.Press_ID = b.Press_ID WHERE b.Sale_Amount > 1000 GROUP BY p.Name

-- Soru 148: What is the average height of paintings for different medium types?
SELECT medium, AVG(height_mm) AS average_height FROM Paintings GROUP BY medium

-- Soru 149: What is the type of powertrain with most number of vehicles.
SELECT Type_of_powertrain, COUNT(*) AS Number_of_Vehicles FROM Vehicles GROUP BY Type_of_powertrain ORDER BY Number_of_Vehicles DESC LIMIT 1

-- Soru 150: Show all the order dates and ids of the orders with quantity of any product larger than 6 or with more than 3 products.
SELECT o.order_date, o.order_id FROM Customer_Orders o JOIN Order_Details od ON o.order_id = od.order_id WHERE od.quantity > 6 OR o.product_count > 3

-- Soru 151: How many countries do we have?
SELECT COUNT(*) AS NumberOfCountries FROM Country

-- Soru 152: Find the top three dates with the most receipts.
SELECT Date, COUNT(ReceiptNumber) AS ReceiptCount FROM receipts GROUP BY Date ORDER BY ReceiptCount DESC LIMIT 3

-- Soru 153: Which region has the largest population? Give me the capital of the region.
SELECT r.Name AS Region_Name, r.Capital FROM region r ORDER BY r.Population DESC LIMIT 1

-- Soru 154: What are the names of pilots who are younger than 35 and have a plane named Piper Cub?
SELECT T1.pilot_name FROM PilotSkills AS T1 JOIN Hangar AS T2 ON T1.plane_name = T2.plane_name WHERE T1.age < 35 AND T2.location = 'Piper Cub'

-- Soru 155: What are the different developers of games that are played by players that attend Auburn college?
SELECT DISTINCT T1.Developers FROM game AS T1 JOIN player AS T2 ON T1.Platform_ID = T2.Platform_ID WHERE T2.College = 'Auburn'

-- Soru 156: How many drivers receive points greater than 150 for each make? Show the make and the count.
SELECT T2.Make, COUNT(*) as Driver_Count FROM driver AS T1 JOIN team AS T2 ON T1.Make = T2.Make WHERE T1.Points > 150 GROUP BY T2.Make

-- Soru 157: List all Planets' names and coordinates in alphabetical order of name.
SELECT Name, Coordinates FROM Planet ORDER BY Name ASC

-- Soru 158: What are the names of everbody who has participated in both the ACL and NACCL conferences?
SELECT s.name FROM staff s JOIN conference_pa cp ON s.staff_id = cp.staff_id JOIN conference c ON cp.conference_id = c.conference_id WHERE c.conference_name IN ('ACL', 'NACCL') GROUP BY s.name HAVING COUNT(DISTINCT c.conference_name) > 1

-- Soru 159: List all payment ids and its corresponding invoice ids and details.
SELECT payment_id, invoice_id, details FROM Payments

-- Soru 160: Count the number of clubs.
SELECT count(*) FROM CLub

-- Soru 161: How many paintings are exhibited at gallery 240?
SELECT COUNT(*) FROM Paintings WHERE location = 'gallery 240'

-- Soru 162: What is detail of collection subset with name 'Top collection'?
SELECT Collecrtion_Subset_Details FROM Collection_Subsets WHERE Collection_Subset_Name = 'Top collection'

-- Soru 163: Which venue has the fewest publications?
SELECT a.name AS venue_name, COUNT(DISTINCT p.paper_id) AS publication_count FROM Affiliation a JOIN Author_list al ON a.affiliation_id = al.affiliation_id JOIN Paper p ON al.paper_id = p.paper_id GROUP BY a.name ORDER BY publication_count ASC LIMIT 1

-- Soru 164: What are the earnings of players from either of the countries of Australia or Zimbabwe?
SELECT Earnings FROM player WHERE Country = 'Australia' OR Country = 'Zimbabwe'

-- Soru 165: Show all client ids and details with sic code "Bad".
SELECT client_id, client_details FROM Clients WHERE sic_code = 'Bad'

-- Soru 166: How many shipments take place on each planet?
SELECT p.Name AS PlanetName, COUNT(s.ShipmentID) AS NumberOfShipments FROM Planet p JOIN Shipment s ON p.PlanetID = s.Planet GROUP BY p.PlanetID

-- Soru 167: List the names and languages of the songs .
SELECT name , language FROM songs

-- Soru 168: Find the name and Citation point of the universities whose reputation points are top 3 and above.
SELECT u.University_Name, o.Citation_point FROM university u JOIN overall_ranking o ON u.University_ID = o.University_ID WHERE o.Reputation_point >= (SELECT Reputation_point FROM overall_ranking ORDER BY Reputation_point DESC LIMIT 3)

-- Soru 169: Find all distinct locations of warehouses.
SELECT DISTINCT LOCATION FROM Warehouses

-- Soru 170: Return the affiliations of instituions that are not in the city of Vancouver.
SELECT Affiliation FROM institution WHERE City != 'Vancouver'

-- Soru 171: Which nationality does the most customers have?
SELECT Nationality FROM customer GROUP BY Nationality ORDER BY COUNT(*) DESC LIMIT 1

-- TEST 172 BAGLANTI HATASI

-- Soru 173: What are the color, description and size of the products priced below the maximum price.
SELECT product_color, product_description, product_size FROM Products WHERE product_price < (SELECT MAX(product_price) FROM Products)

-- Soru 174: When was the earliest date of loan?
SELECT MIN(date_of_transcript) AS earliest_date_of_loan FROM Transcripts

-- Soru 175: What is the total number of students enrolled in an university with a rank of 5 or below?
SELECT SUM(u.Enrollment) AS Total_Enrollment FROM university u JOIN overall_ranking o ON u.University_ID = o.University_ID WHERE o.Rank <= 5

-- Soru 176: Find the max age for each group of pilots with the same name.
SELECT pilot_name, MAX(age) AS max_age FROM PilotSkills GROUP BY pilot_name

-- Soru 177: What are the names and id of platforms whose download rank is 1?
SELECT Platform_name , Platform_id FROM platform WHERE Download_rank = 1

-- Soru 178: What is the top speed and power of the vehicle manufactured in the year of 1996?
SELECT Top_Speed, Power FROM vehicle WHERE Build_Year = 1996 ORDER BY Top_Speed DESC LIMIT 1

-- Soru 179: What are the model names and build year of the cars with 'DJ' in its model name?
SELECT model , build_year FROM vehicle WHERE model LIKE '%DJ%'

-- Soru 180: How many customers have bought each food?
SELECT g.Food, COUNT(DISTINCT r.CustomerId) AS NumberOfCustomers FROM receipts r JOIN items i ON r.ReceiptNumber = i.Receipt JOIN goods g ON i.Item = g.Id GROUP BY g.Food

-- Soru 181: List names of all authors who have only 1 paper.
SELECT a.name FROM Author a JOIN Author_list al ON a.author_id = al.author_id GROUP BY a.author_id HAVING COUNT(al.paper_id) = 1

-- Soru 182: In which years were more than one institution founded?
SELECT founded FROM institution GROUP BY founded HAVING count(*) > 1

-- Soru 183: What is the transmitter of the radio with the largest ERP_kW?
SELECT Transmitter FROM radio ORDER BY ERP_kW DESC LIMIT 1

-- Soru 184: What are highest, lowest, and average prices of goods, grouped and ordered by flavor?
SELECT Flavor, MAX(Price) AS Highest_Price, MIN(Price) AS Lowest_Price, AVG(Price) AS Average_Price FROM goods GROUP BY Flavor

-- Soru 185: For each conference id, what are their names, year, and number of participants?
SELECT c.Conference_ID, c.Conference_Name, c.Year, COUNT(cp.Participant_ID) AS Number_of_Participants

-- Soru 186: What are the full names of customers who bought apple flavored Tarts?
SELECT T1.FirstName, T1.LastName FROM customers AS T1 JOIN receipts AS T2 ON T1.Id = T2.CustomerId JOIN items AS T3 ON T2.ReceiptNumber = T3.Receipt WHERE T3.Item = 'apple flavored Tarts'

-- Soru 187: List all product names in descending order of price.
SELECT product_name FROM Products ORDER BY product_price DESC

-- Soru 188: How many products are there for each product type?
SELECT product_type_code, COUNT(*) AS number_of_products FROM Products GROUP BY product_type_code

-- Soru 189: What are the constructors who are used by both drivers who are younger than 20 and drivers older than 30?
SELECT Constructor FROM driver WHERE Age < 20 INTERSECT SELECT Constructor FROM driver WHERE Age > 30

-- Soru 190: List names and addresses for all affiliations.
SELECT name, address FROM Affiliation

-- Soru 191: What are all the distinct franchises?
SELECT DISTINCT Franchise FROM game

-- Soru 192: How many distinct planes are owned across all pilots?
SELECT count(DISTINCT plane_name) FROM PilotSkills

-- Soru 193: Which region has the largest number of buildings? Show me the capital of the region.
SELECT r.Name AS Region_Name, r.Capital FROM region r JOIN ( SELECT Region_ID, COUNT(*) as Building_Count FROM building GROUP BY Region_ID ) sub ON r.Region_ID = sub.Region_ID ORDER BY sub.Building_Count DESC LIMIT 1

-- Soru 194: What is the title and purchase price of the book that has the highest total order amount?
SELECT T3.Title, T3.PurchasePrice FROM Orders AS T1 INNER JOIN OrderDetails AS T2 ON T1.IdOrder = T2.IdOrder INNER JOIN Book AS T3 ON T2.ISBN = T3.ISBN GROUP BY T2.ISBN ORDER BY SUM(T2.Quantity * T3.PurchasePrice) DESC LIMIT 1

-- Soru 195: What are the maximum and minimum ages for all staff?
SELECT MAX(Age), MIN(Age) FROM staff

-- Soru 196: What are the names of all universities without any majors ranked number 1?
SELECT University_Name FROM university WHERE University_ID NOT IN ( SELECT University_ID FROM overall_ranking WHERE Rank = 1 )

-- Soru 197: What are the unique first names of the artists who had medium oil paintings located in gallery 241?
SELECT DISTINCT T1.fname FROM Artists AS T1 JOIN Paintings AS T2 ON T1.artistID = T2.painterID WHERE T2.medium = 'oil' AND T2.location = 241

-- Soru 198: What are the ids of sailors who reserved red and blue boats?
SELECT T1.sid FROM Reserves AS T1 JOIN Boats AS T2 ON T1.bid = T2.bid WHERE T2.color = 'red' INTERSECT SELECT T1.sid FROM Reserves AS T1 JOIN Boats AS T2 ON T1.bid = T2.bid WHERE T2.color = 'blue'

-- Soru 199: What is the discount name with most number of renting history records?
SELECT D.name FROM Discount D JOIN Renting_History RH ON D.id = RH.discount_id GROUP BY D.name ORDER BY COUNT(RH.id) DESC LIMIT 1

-- Soru 200: Find the name of the publisher whose monthly profit is the highest or the lowest.
SELECT Name FROM press ORDER BY Month_Profits_billion DESC, Month_Profits_billion ASC LIMIT 2

-- Soru 201: Find the titles and publish dates of the top 5 best sale books.
SELECT Title, Release_date FROM book ORDER BY CAST(REPLACE(Sale_Amount, ',', '') AS REAL) DESC LIMIT 5

-- Soru 202: What is the receipt number with the latest date, and what is that date?
SELECT T1.ReceiptNumber , T1.Date FROM receipts AS T1 ORDER BY T1.Date DESC LIMIT 1

-- Soru 203: Return the client ids for clients with two or more invoices?
SELECT client_id FROM Invoices GROUP BY client_id HAVING COUNT(invoice_id) >= 2

-- Soru 204: What is the first name of the user who owns the greatest number of properties?
SELECT u.first_name, COUNT(p.property_id) AS property_count FROM Users u JOIN Properties p ON u.user_id = p.owner_id GROUP BY u.first_name ORDER BY property_count DESC LIMIT 1

-- Soru 205: Show name and model year for vehicles with city fuel economy rate less than or equal to highway fuel economy rate.
SELECT name , model_year FROM Vehicles WHERE city_fuel_economy_rate <= highway_fuel_economy_rate

-- Soru 206: What are the different room sizes, and how many of each are there?
SELECT room_size, COUNT(*) AS number_of_rooms FROM Rooms GROUP BY room_size

-- Soru 207: Show the customers with total quantity of order bigger than 1.
SELECT c.Customer_ID, c.Name, SUM(co.Quantity) AS Total_Quantity FROM customer c JOIN customer_order co ON c.Customer_ID = co.Customer_ID GROUP BY c.Customer_ID, c.Name HAVING SUM(co.Quantity) > 1

-- Soru 208: Show me the city code of two cities with maximum distance.
SELECT city1_code, city2_code, MAX(distance) AS max_distance FROM Direct_distance

-- Soru 209: What is the name of the country with the highest politics score?
SELECT name FROM countries ORDER BY politics_score DESC LIMIT 1

-- Soru 210: Please show the names of drivers and the names of races they participate in.
SELECT T1.Driver_Name , T2.Race_Name FROM driver AS T1 JOIN race AS T2 ON T1.Driver_ID = T2.Driver_ID

-- Soru 211: What are all the distinct states?
SELECT DISTINCT state FROM City

-- Soru 212: Find the number of reservations for each boat with more than 1 reservation.
SELECT b.name, COUNT(r.bid) AS num_reservations FROM Boats b JOIN Reserves r ON b.bid = r.bid GROUP BY b.name HAVING COUNT(r.bid) > 1

-- Soru 213: What are the names of every theater with at least one movie playing?
SELECT T2.Name FROM MovieTheaters AS T1 JOIN MovieTheaters AS T2 ON T1.Code = T2.Movie GROUP BY T2.Name HAVING COUNT(*) >= 1

-- Soru 214: Show all conference names and the number of times each conference has.
SELECT Conference_Name, COUNT(*) AS Number_of_Times FROM conference GROUP BY Conference_Name

-- Soru 215: Which headphone class contains the most headphones?
SELECT Class, COUNT(*) AS Number_of_Headphones FROM headphone GROUP BY Class ORDER BY Number_of_Headphones DESC LIMIT 1

-- Soru 216: How many hardware type products do we have?
SELECT COUNT(*) FROM Products WHERE product_type_code = 'hardware'

-- Soru 217: What is the code and description of the most common student address type?
CREATE TABLE Student_Addresses ( address_id number PRIMARY KEY, code text, description text, student_id number, FOREIGN KEY (student_id) REFERENCES Students(student_id) )

-- Soru 218: Find the most common result in the behavioral monitoring details. What are the count and the details of this result?
SELECT behaviour_monitoring_details, COUNT(*) as count FROM Behaviour_Monitoring GROUP BY behaviour_monitoring_details ORDER BY count DESC LIMIT 1

-- Soru 219: Show all sic codes and the number of clients with each code.
SELECT sic_code, COUNT(client_id) AS number_of_clients FROM Clients GROUP BY sic_code

-- Soru 220: What are the names of the collections that are not the parent of the other collections?
SELECT Collection_Subset_Name FROM Collection_Subsets WHERE Collection_Subset_ID NOT IN ( SELECT Parent_Collection_Subset_ID FROM Collection_Subsets )

-- Soru 221: For every order, how many products does it contain, and what are the orders' statuses and ids?
SELECT COUNT(*), T1.status, T1.id FROM Orders AS T1 JOIN Order_items AS T2 ON T1.id = T2.order_id GROUP BY T1.id

-- Soru 222: Find the names of all sculptures located in gallery 226.
SELECT title FROM Sculptures WHERE location = 'gallery 226'

-- Soru 223: What are the names of pilots, ordered by age descending?
SELECT pilot_name FROM pilotskills ORDER BY age DESC

-- Soru 224: How many teachers have taught a student who has not won any achievements?
SELECT COUNT(DISTINCT T2.teacher_id) FROM Students AS T1 JOIN Transcripts AS T2 ON T1.student_id = T2.student_id LEFT JOIN Achievements AS T3 ON T1.student_id = T3.student_id WHERE T3.achievement_id IS NULL

-- Soru 225: What are the names of all sculptures in gallery 226?
SELECT title FROM Sculptures WHERE location = 'gallery 226'

-- Soru 226: What are the distinct descriptions of all the detentions which have ever happened?
SELECT DISTINCT T2.behaviour_monitoring_details FROM Behaviour_Monitoring AS T1 JOIN Students AS T2 ON T1.student_id = T2.student_id

-- Soru 227: What is the country of the player with the highest earnings among players that have more than 2 win counts?
SELECT Country FROM player WHERE Wins_count > 2 ORDER BY Earnings DESC LIMIT 1

-- Soru 228: List the affiliations shared by more than three city channels.
SELECT Affiliation FROM city_channel GROUP BY Affiliation HAVING COUNT(*) > 3

-- Soru 229: Count the number of property photos each property has by id.
SELECT p.property_id, COUNT(ph.photo_id) AS num_photos FROM Properties p LEFT JOIN Photos ph ON p.property_id = ph.property_id GROUP BY p.property_id

-- Soru 230: What are the client details for each client and the corresponding details of their agencies?
SELECT Clients.client_details, Agencies.agency_details FROM Clients JOIN Agencies ON Clients.agency_id = Agencies.agency_id

-- Soru 231: Return the names of directors who are older than the average age.
SELECT Name FROM director WHERE Age > (SELECT AVG(Age) FROM director)

-- Soru 232: What is the type of event the most students joined? Give me the number of students, and the event type code and description.
SELECT event_type_code, event_type_description, COUNT(*) as num_students FROM Events JOIN Student_Events ON Events.event_id = Student_Events.event_id GROUP BY event_type_code, event_type_description ORDER BY num_students DESC LIMIT 1

-- Soru 233: What are the names and original air dates of tv shows?
SELECT tv_show_name , original_airdate FROM tv_show

-- Soru 234: What are the names of the movies not being shown in any theaters?
SELECT Title FROM Movies WHERE Code NOT IN (SELECT Movie FROM MovieTheaters)

-- Soru 235: List the invoices' status, date and the date of shipment.
Unfortunately, the provided database schema does not include any tables related to invoices or shipments. Therefore, it's impossible to provide an accurate SQL query based on the given information. Please provide a more complete database schema that includes relevant tables for this question.

-- Soru 236: What are the names, cities, and states of all universities in alphabetical order (by name of the university).
SELECT University_Name, City, State FROM university ORDER BY University_Name

-- Soru 237: Who is the owner of the parent document of every documents where 'Marlin' is the owner?
WITH MarlinDocuments AS ( SELECT Document_Object_ID FROM Document_Objects WHERE Owner = 'Marlin' ), ParentDocuments AS ( SELECT Parent_Document_Object_ID FROM Document_Objects WHERE Document_Object_ID IN (SELECT Document_Object_ID FROM MarlinDocuments) ), ParentOwners AS ( SELECT Owner FROM Document_Objects WHERE Document_Object_ID IN (SELECT Parent_Document_Object_ID FROM ParentDocuments) ) SELECT DISTINCT Owner FROM ParentOwners

-- Soru 238: Which employees do not have clearance in Mars? List employee's name.
-- Step 1: Get the planet ID for Mars WITH Mars_Planet AS ( SELECT PlanetID FROM Planet WHERE Name = 'Mars' ), -- Step 2: Find all employees who have clearance on Mars Employees_With_Clearance AS ( SELECT EmployeeID FROM Has_Clearance WHERE Planet = (SELECT PlanetID FROM Mars_Planet) ) -- Step 3: List all employees and exclude those who have clearance on Mars SELECT Name FROM Employee WHERE EmployeeID NOT IN (SELECT EmployeeID FROM Employees_With_Clearance)

-- Soru 239: What are the names of authors who have not published a paper in ACL?
SELECT name FROM Author WHERE author_id NOT IN (SELECT DISTINCT author_id FROM Author_list WHERE paper_id IN (SELECT paper_id FROM Paper WHERE acronym = 'ACL'))

-- Soru 240: Which students take 2 courses? List student id and details.
SELECT s.student_id, s.bio_data, s.student_details FROM Students s JOIN Transcripts t ON s.student_id = t.student_id GROUP BY s.student_id HAVING COUNT(t.transcript_id) = 2

-- Soru 241: What are the titles of books with the highest purchase price across all books ?
SELECT Title FROM Book WHERE PurchasePrice = (SELECT MAX(PurchasePrice) FROM Book)

-- Soru 242: How many different official languages are there?
SELECT COUNT(DISTINCT language_id) AS number_of_official_languages FROM official_languages

-- Soru 243: What are the names of all authors who have more than 50 papers?
SELECT a.name FROM Author a JOIN Author_list al ON a.author_id = al.author_id GROUP BY a.author_id HAVING COUNT(al.paper_id) > 50

-- Soru 244: What are the average city fuel economy rate, average highway fuel economy rate for different types of powertrains?
SELECT Type_of_powertrain, AVG(City_fuel_economy_rate) AS Average_City_Fuel_Economy, AVG(Highway_fuel_economy_rate) AS Average_Highway_Fuel_Economy FROM Vehicles GROUP BY Type_of_powertrain

-- Soru 245: What are the names of all players in alphabetical order?
SELECT Player_name FROM player ORDER BY Player_name ASC

-- Soru 246: Count the number of customers.
SELECT COUNT(*) FROM Customers

-- Soru 247: What are all payment ids and payment details for invoices with status Working?
SELECT p.payment_id, p.payment_details FROM Payments p JOIN Invoices i ON p.invoice_id = i.invoice_id WHERE i.invoice_status = 'Working'

-- Soru 248: Find the title of the movie that is played in the Odeon theater.
SELECT T2.Title FROM MovieTheaters AS T1 JOIN Movies AS T2 ON T1.Movie = T2.Code WHERE T1.Name = 'Odeon'

-- Soru 249: Find the name and age of pilots who have a plane in Austin.
SELECT T1.pilot_name , T1.age FROM PilotSkills AS T1 JOIN Hangar AS T2 ON T1.plane_name = T2.plane_name WHERE T2.location = 'Austin'

-- Soru 250: Select the area and government website of the district with the smallest population.
SELECT Area_km, Government_website FROM district ORDER BY Population ASC LIMIT 1

-- Soru 251: Count the number of affiliations.
SELECT COUNT(*) FROM Affiliation

-- Soru 252: When was the transcript issued for the student with loan of maximum value?
SELECT t.date_of_transcript FROM Transcripts t JOIN Students s ON t.student_id = s.student_id JOIN Loans l ON s.student_id = l.student_id ORDER BY l.loan_value DESC LIMIT 1

-- Soru 253: Find the highest and lowest points of drivers.
SELECT MAX(Points) AS Highest_Points, MIN(Points) AS Lowest_Points FROM driver

-- Soru 254: What are the shipment ids for the planet Mars?
SELECT T1.ShipmentID FROM Shipment AS T1 JOIN Planet AS T2 ON T1.Planet = T2.PlanetID WHERE T2.Name = "Mars"

-- Soru 255: What are the stadiums of institutions in descending order of the capacity.
SELECT Stadium FROM institution ORDER BY Capacity DESC

-- Soru 256: Show all city names and the average distance to all other cities.
SELECT C1.city_name, AVG(C2.distance) AS avg_distance FROM City C1 JOIN Direct_distance C2 ON C1.city_code = C2.city1_code GROUP BY C1.city_name

-- Soru 257: Which service ( s ) has never been used by any customer ? List their details .
SELECT s.Service_ID, s.Service_Details FROM Services s LEFT JOIN Customers_and_Services cas ON s.Service_ID = cas.Service_ID WHERE cas.Customer_ID IS NULL

-- Soru 258: How many exams are there?
SELECT COUNT(*) FROM Exams

-- Soru 259: Find the exams whose subject code is not "Database". What are the exam dates and exam names?
SELECT Exam_Date, Exam_Name FROM Exams WHERE Subject_Code != 'Database'

-- Soru 260: What are names of drivers who did not take part in a race?
SELECT Driver_Name FROM driver WHERE Driver_ID NOT IN (SELECT Driver_ID FROM race)

-- Soru 261: What are the names and ids of all sailors who have a rating of at least 3 and reserved a boat?
SELECT name , sid FROM sailors WHERE rating >= 3 AND sid IN (SELECT sid FROM reserves)

-- Soru 262: List the document id of all documents in collection named Best.
SELECT Document_Object_ID FROM Collection_Document_Membership JOIN Collections ON Collection_Document_Membership.Collection_ID = Collections.Collection_ID WHERE Collections.Collection_Name = 'Best'

-- Soru 263: Find the name and id of Sailors (sid) that reserved red and blue boat.
SELECT s.sid, s.name FROM Sailors s WHERE EXISTS ( SELECT 1 FROM Reserves r JOIN Boats b ON r.bid = b.bid WHERE r.sid = s.sid AND b.color = 'red' ) AND EXISTS ( SELECT 1 FROM Reserves r JOIN Boats b ON r.bid = b.bid WHERE r.sid = s.sid AND b.color = 'blue' )

-- Soru 264: Return all details of sailors who are older than 30.
SELECT * FROM Sailors WHERE age > 30

-- Soru 265: Find the headphone models that are not in stock in any store.
SELECT h.Model FROM headphone h LEFT JOIN stock s ON h.Headphone_ID = s.Headphone_ID WHERE s.Quantity IS NULL

-- Soru 266: Show all home conferences and the number of universities in each conference.
SELECT Home_Conference, COUNT(*) AS Number_of_Universities FROM university GROUP BY Home_Conference

-- Soru 267: What are the ids of goods that cost less than 3 dollars?
SELECT id FROM goods WHERE price < 3

-- Soru 268: What is the most uncommon order status?
SELECT order_status, COUNT(*) AS status_count FROM Orders GROUP BY order_status ORDER BY status_count ASC LIMIT 1

-- Soru 269: What are the names of the different official languages, as well as the number of countries that speak each?
SELECT T1.name , count(*) FROM languages AS T1 JOIN official_languages AS T2 ON T1.id = T2.language_id GROUP BY T2.language_id

-- Soru 270: Please list the names of races with drivers aged 26 or older participating.
SELECT T2.Race_Name FROM driver AS T1 JOIN race AS T2 ON T1.Driver_ID = T2.Driver_ID WHERE T1.Age >= 26

-- Soru 271: Give the different entrant types for drivers at least 20 years old.
SELECT DISTINCT Entrant FROM driver WHERE Age >= 20

-- Soru 272: What are the type of questions and their counts?
SELECT Type_of_Question_Code, COUNT(*) AS Count FROM Questions GROUP BY Type_of_Question_Code

-- Soru 273: List all program titles in the order of starting year. List the most recent one first.
SELECT Title FROM program ORDER BY Start_Year DESC

-- Soru 274: What is the sum of distances between BAL and other cities?
SELECT SUM(distance) FROM Direct_distance JOIN City ON Direct_distance.city1_code = City.city_code OR Direct_distance.city2_code = City.city_code WHERE City.cname = 'BAL'

-- Soru 275: Find the ids of goods that have apple flavor.
SELECT Id FROM goods WHERE Flavor = "apple"

-- Soru 276: What are the ids of the documents that have more than one child?
SELECT Parent_Document_Object_ID FROM Document_Objects GROUP BY Parent_Document_Object_ID HAVING COUNT(*) > 1

-- Soru 277: What are the manager and car owner of the team that has at least 2 drivers?
SELECT t.Manager, t.Car_Owner FROM team t JOIN driver d ON t.Team_ID = d.Car_# GROUP BY t.Team_ID, t.Manager, t.Car_Owner HAVING COUNT(d.Driver_ID) >= 2

-- Soru 278: What is the maximum, average, and minimum enrollment for all universities?
SELECT MAX(enrollment), AVG(enrollment), MIN(enrollment) FROM university

-- Soru 279: List the last names of the students whose gender is "F" or "M".
SELECT Last_Name FROM Students WHERE Gender_MFU = "F" OR Gender_MFU = "M"

-- Soru 280: For those students who have gone through an event, who do not have a student loan? List the students' biographical data
SELECT s.student_id, s.bio_data, s.student_details FROM Students s JOIN Transcripts t ON s.student_id = t.student_id LEFT JOIN Student_Loans sl ON s.student_id = sl.student_id WHERE t.date_of_transcript IS NOT NULL AND sl.student_id IS NULL

-- Soru 281: For each analytical layer, return the analytical layer type code and the number of times it was used.
The provided database schema does not include any table or column related to "analytical layer" or "analytical layer type code". Therefore, it's impossible to generate a SQL query based on the given natural language question. Please provide more accurate information about your database schema.

-- Soru 282: What are the names and coordinates of all planets in alphabetical order by name?
SELECT Name, Coordinates FROM Planet ORDER BY Name ASC

-- Soru 283: Return the name of the oldest pilot.
SELECT pilot_name FROM PilotSkills ORDER BY age DESC LIMIT 1

-- Soru 284: What are the names and ages of all sailors sorted by decreasing rating?
SELECT name , age FROM sailors ORDER BY rating DESC

-- Soru 285: What is the product type with least number of products?
SELECT product_type_code FROM Products GROUP BY product_type_code ORDER BY COUNT(*) ASC LIMIT 1

-- Soru 286: What are the agency ids and details agencies with at least 2 clients?
SELECT a.agency_id, a.agency_details FROM Agencies a JOIN Clients c ON a.agency_id = c.agency_id GROUP BY a.agency_id, a.agency_details HAVING COUNT(c.client_id) >= 2

-- Soru 287: Find all the customers whose name contains "Kutch".
SELECT * FROM Customers WHERE Customer_Details LIKE '%Kutch%'

-- Soru 288: What are the number of shipments managed and names of each manager?
SELECT COUNT(T2.ShipmentID) AS Number_of_Shipments, T1.Name AS Manager_Name FROM Employee AS T1 JOIN Shipment AS T2 ON T1.EmployeeID = T2.Manager GROUP BY T1.Name

-- Soru 289: How many different neighborhoods are there for all stores?
SELECT COUNT(DISTINCT Neighborhood) AS Number_of_Neighborhoods FROM store

-- Soru 290: Count the number of payments.
SELECT COUNT(*) AS total_payments FROM Invoices

-- Soru 291: What is the name and rank of every university in Wisconsin?
SELECT T1.University_Name , T2.rank FROM university AS T1 JOIN overall_ranking AS T2 ON T1.university_id = T2.university_id WHERE T1.state = 'Wisconsin'

-- Soru 292: Return the names of drivers with citizenship from the United States.
SELECT Name FROM driver WHERE Citizenship = 'United States'

-- Soru 293: How many boats are red?
SELECT count(*) FROM Boats WHERE color = 'red'

-- Soru 294: Return the stadiums of institutions, ordered by capacity descending.
SELECT stadium FROM institution ORDER BY capacity DESC

-- Soru 295: Find the number of books that are sold more than 1000 for each publisher. List the press name as well.
SELECT p.Name, COUNT(b.Book_ID) as Number_of_Books_Sold FROM book b JOIN press p ON b.Press_ID = p.Press_ID WHERE b.Sale_Amount > 1000 GROUP BY p.Name

-- Soru 296: Find the number of distinct types of contents in each warehouse.
SELECT count(DISTINCT contents) , T1.code FROM warehouses AS T1 JOIN boxes AS T2 ON T1.code = T2.warehouse GROUP BY T1.code

-- Soru 297: How many planes are owned by the pilot whose name is Smith?
SELECT count(*) FROM Pilotskills WHERE pilot_name = "Smith"

-- Soru 298: What are the dates of the exams whose subject code contains the substring "data"? Return them in descending order of dates.
SELECT Exam_Date FROM Exams WHERE Subject_Code LIKE '%data%' ORDER BY Exam_Date DESC

-- Soru 299: How much do the products cost on average?
SELECT AVG(product_price) AS average_product_price FROM Products

-- Soru 300: What is the age of the driver who raced in the most races?
SELECT T1.Age FROM driver AS T1 JOIN race AS T2 ON T1.Driver_ID = T2.Driver_ID GROUP BY T1.Driver_ID ORDER BY COUNT(*) DESC LIMIT 1

-- Soru 301: How many different places have some plane?
SELECT count(DISTINCT LOCATION) FROM Hangar

-- Soru 302: What types of contents cannot be found in warehouses in New York?
SELECT DISTINCT contents FROM boxes EXCEPT SELECT DISTINCT contents FROM boxes AS T1 JOIN warehouses AS T2 ON T1.warehouse = T2.code WHERE LOCATION = 'New York'

-- Soru 303: List the country that have more than one player.
SELECT Country FROM player GROUP BY Country HAVING COUNT(Player_ID) > 1

-- Soru 304: How many drivers have not driven any cars?
SELECT COUNT(*) FROM driver WHERE Driver_ID NOT IN (SELECT Driver_ID FROM vehicle_driver)

-- Soru 305: Find all receipts in which either apple flavor pie was bought or customer id 12 shopped.
SELECT T1.ReceiptNumber FROM receipts AS T1 JOIN items AS T2 ON T1.ReceiptNumber = T2.Receipt WHERE T2.Item = 'apple' UNION SELECT T1.ReceiptNumber FROM receipts AS T1 JOIN items AS T2 ON T1.ReceiptNumber = T2.Receipt WHERE T1.CustomerId = 12

-- Soru 306: For each location, what are the total, average, and maximum capacities of warehouses?
SELECT LOCATION , sum(Capacity) , avg(Capacity) , max(Capacity) FROM WAREHOUSES GROUP BY LOCATION

-- Soru 307: Which teacher teaches the most courses? Give me the id of the teacher and the number of courses he or she teaches.
SELECT T2.teacher_id, COUNT(*) as course_count FROM Courses AS T1 JOIN Teachers AS T2 ON T1.teacher_id = T2.teacher_id GROUP BY T2.teacher_id ORDER BY course_count DESC LIMIT 1

-- Soru 308: For each building, return the address of the building and the name of the region it belongs to.
SELECT T1.Address , T2.Name FROM building AS T1 JOIN region AS T2 ON T1.Region_ID = T2.Region_ID

-- Soru 309: For each city, what is the the city code and sum of distances from each?
SELECT c.city_code, SUM(dd.distance) AS total_distance FROM City c JOIN Direct_distance dd ON c.city_code = dd.city1_code OR c.city_code = dd.city2_code GROUP BY c.city_code

-- Soru 310: What is the name of the planet with the most shipments?
SELECT p.Name FROM Planet p JOIN Shipment s ON p.PlanetID = s.Planet GROUP BY p.PlanetID ORDER BY COUNT(s.ShipmentID) DESC LIMIT 1

-- Soru 311: Find the titles and paper IDs for papers which have Mckeown but not Rambow in author list.
SELECT DISTINCT p.title, al.paper_id FROM Paper p JOIN Author_list al ON p.paper_id = al.paper_id JOIN Author a ON al.author_id = a.author_id WHERE a.name = 'Mckeown' AND NOT EXISTS ( SELECT 1 FROM Author_list al2 JOIN Author a2 ON al2.author_id = a2.author_id WHERE al2.paper_id = p.paper_id AND a2.name = 'Rambow' )

-- Soru 312: Find the average city and highway fuel rates for cars with different powertrain types.
SELECT Type_of_powertrain, AVG(City_fuel_economy_rate) AS Average_City_Fuel_Rate, AVG(Highway_fuel_economy_rate) AS Average_Highway_Fuel_Rate FROM Vehicles GROUP BY Type_of_powertrain

-- Soru 313: For every home conference, how many universities attended that conference?
SELECT Home_Conference, COUNT(*) AS Number_of_Universities FROM university GROUP BY Home_Conference

-- Soru 314: Show me the city code of two cities with a distance less than 1000.
SELECT city1_code, city2_code FROM Direct_distance WHERE distance < 1000

-- Soru 315: What are the number of packages shipped on Omicron Persei 8 planet or sent by Zapp Brannigan?
SELECT COUNT(*) FROM Shipment AS T1 JOIN Planet AS T2 ON T1.Planet = T2.PlanetID JOIN Employee AS T3 ON T1.Manager = T3.EmployeeID WHERE T2.Name = 'Omicron Persei 8' OR T3.Name = 'Zapp Brannigan'

-- Soru 316: Show the capital of the region that has the most buildings.
SELECT r.Capital FROM region r JOIN building b ON r.Region_ID = b.Region_ID GROUP BY r.Region_ID ORDER BY COUNT(b.Building_ID) DESC LIMIT 1

-- Soru 317: What are all the city names for cities in the USA, ordered by latitude?
SELECT T1.cname FROM city AS T1 JOIN country AS T2 ON T1.country_code = T2.code WHERE T2.continent = 'North America' ORDER BY T1.latitude

-- Soru 318: What are the managers and captains of clubs?
SELECT Manager , Captain FROM club

-- Soru 319: Which country has least number of students?
SELECT country_code, COUNT(*) AS num_students FROM Student GROUP BY country_code ORDER BY num_students ASC LIMIT 1

-- Soru 320: Show the titles of books that have been ordered.
SELECT DISTINCT B.Title FROM Orders O JOIN Order_Books OB ON O.IdOrder = OB.IdOrder JOIN Book B ON OB.ISBN = B.ISBN

-- Soru 321: What are the maximum and minimum points of drivers.
SELECT MAX(Points) AS Max_Points, MIN(Points) AS Min_Points FROM driver

-- Soru 322: How many drivers have points smaller than 150?
SELECT COUNT(*) FROM driver WHERE Points < 150

-- Soru 323: Find the top 2 earpads that are mostly used.
SELECT Earpads, COUNT(*) as Usage_Count FROM headphone GROUP BY Earpads ORDER BY Usage_Count DESC LIMIT 2

-- Soru 324: Return the number of orders received for Pride and Prejudice.
SELECT COUNT(*) FROM Orders o JOIN Book b ON o.ISBN = b.ISBN WHERE b.Title = 'Pride and Prejudice'

-- Soru 325: Please show the names and rank of players that have played the game titled "Super Mario World".
SELECT p.Player_name, p.Rank_of_the_year FROM player p JOIN player_game_activity pga ON p.Player_ID = pga.Player_ID JOIN game g ON pga.Game_ID = g.Game_ID WHERE g.Title = 'Super Mario World'

-- Soru 326: When was the time of the latest search by a user?
SELECT MAX(search_time) AS latest_search_time FROM Searches

-- Soru 327: What are the distinct ids of customers who bought lemon flavored cake?
SELECT DISTINCT T1.CustomerId FROM receipts AS T1 JOIN items AS T2 ON T1.ReceiptNumber = T2.Receipt WHERE T2.Item = 'cake' AND T2.Ordinal = 1 AND T1.Date LIKE '%lemon%'

-- Soru 328: Find the make of the team whose car owner is "Buddy Arrington".
SELECT Make FROM team WHERE Car_Owner = 'Buddy Arrington'

-- Soru 329: How many invoices are there for each status code?
SELECT invoice_status, COUNT(*) AS number_of_invoices FROM Invoices GROUP BY invoice_status

-- Soru 330: What are the names of drivers who had both the pole position James Hinchcliffe and the pole position Carl Skerlong?
SELECT T1.Driver_Name FROM driver AS T1 JOIN race AS T2 ON T1.Driver_ID = T2.Driver_ID WHERE T2.Pole_Position = 'James Hinchcliffe' INTERSECT SELECT T1.Driver_Name FROM driver AS T1 JOIN race AS T2 ON T1.Driver_ID = T2.Driver_ID WHERE T2.Pole_Position = 'Carl Skerlong'

-- Soru 331: List the product names and weights of the bikes in ascending order of price.
SELECT product_name , weight FROM bike ORDER BY price ASC

-- Soru 332: What are the names of authors who have exactly 1 paper?
SELECT a.name FROM Author a JOIN Author_list al ON a.author_id = al.author_id GROUP BY a.author_id HAVING COUNT(al.paper_id) = 1

-- Soru 333: List the names of buildings in ascending order of number of stories.
SELECT Name FROM building ORDER BY Number_of_Stories ASC

-- Soru 334: Which customers have integration platform details "Fail" in interactions? Give me the customer details.
SELECT DISTINCT C.Customer_Details FROM Customers AS C JOIN Customers_and_Services AS CAS ON C.Customer_ID = CAS.Customer_ID JOIN Services AS S ON CAS.Service_ID = S.Service_ID WHERE S.Service_Details = 'Integration Platform' AND CAS.Customers_and_Services_Details = 'Fail'

-- Soru 335: What are the types of books that have at least three books belonging to?
SELECT TYPE FROM book GROUP BY TYPE HAVING COUNT(*) >= 3

-- Soru 336: List the biographical data of the students who never had a detention or student loan .
SELECT bio_data FROM Students WHERE student_id NOT IN ( SELECT student_id FROM Behaviour_Monitoring )

-- Soru 337: What are the colors , descriptions , and sizes for all products that are not at the maximum price ?
SELECT product_color, product_description, product_size FROM Products WHERE product_price < (SELECT MAX(product_price) FROM Products)

-- Soru 338: Return the ids and names of the districts whose population is larger than 4000 or area bigger than 3000.
SELECT District_ID, Name FROM district WHERE Population > 4000 OR Area_km > 3000

-- Soru 339: What are all the flavors of croissant?
SELECT DISTINCT Flavor FROM goods WHERE Food = "croissant"

-- Soru 340: What are the names of planes that the pilot Jones who is 32 has?
SELECT T1.plane_name FROM PilotSkills AS T1 JOIN Hangar AS T2 ON T1.plane_name = T2.plane_name WHERE T1.pilot_name = "Jones" AND T1.age = 32

-- Soru 341: How many packages sent by Ogden Wernstrom and received by Leo Wong?
SELECT COUNT(T2.ShipmentID) FROM Employee AS T1 JOIN Shipment AS T2 ON T1.EmployeeID = T2.Manager WHERE T1.Name = 'Ogden Wernstrom' AND T2.Date IN ( SELECT Date FROM Shipment WHERE Manager = ( SELECT EmployeeID FROM Employee WHERE Name = 'Leo Wong' ) )

-- Soru 342: List distinct receipt numbers for which someone bought a good that costs more than 13 dollars.
SELECT DISTINCT T1.ReceiptNumber FROM receipts AS T1 JOIN items AS T2 ON T1.ReceiptNumber = T2.ReceiptNumber JOIN goods AS T3 ON T2.Item = T3.Id WHERE T3.Price > 13

-- Soru 343: List the names of customers that do not have any order.
SELECT c.Name FROM customer c LEFT JOIN customer_order co ON c.Customer_ID = co.Customer_ID WHERE co.Dish_Name IS NULL

-- Soru 344: What are the tracking numbers and dates for all shipments listed?
SELECT tracking_number , date FROM shipments

-- Soru 345: List the start time, end time of each meeting, and the corresponding client detail and staff detail.
SELECT m.start_time, m.end_time, c.client_details, s.staff_details FROM Meetings m JOIN Clients c ON m.client_id = c.client_id JOIN Staff s ON m.staff_id = s.staff_id

-- Soru 346: Find the names of districts which have more than one spokesman.
SELECT d.Name FROM district d JOIN spokesman_district sd ON d.District_ID = sd.District_ID GROUP BY d.District_ID, d.Name HAVING COUNT(sd.Spokesman_ID) > 1

-- Soru 347: What are the distinct venues for papers, ordered by year?
SELECT DISTINCT T2.name FROM Paper AS T1 JOIN Venue AS T2 ON T1.venue_id = T2.venue_id ORDER BY T1.year

-- Soru 348: What is the title of the paper that has most number of authors?
SELECT p.title FROM Paper p JOIN Author_list al ON p.paper_id = al.paper_id GROUP BY p.paper_id, p.title ORDER BY COUNT(al.author_id) DESC LIMIT 1

-- Soru 349: In how many different warehouses are Rocks stored within boxes?
SELECT count(DISTINCT warehouse) FROM boxes WHERE contents = 'Rocks'

-- Soru 350: Show all staff names who attend a conference in 2003 or 2004.
SELECT s.name FROM staff s JOIN conference_pa cp ON s.staff_id = cp.staff_id WHERE cp.Conference_ID IN ( SELECT Conference_ID FROM conference WHERE Year IN (2003, 2004) )

-- Soru 351: What are the invoice ids and statuses for invoices without a payment?
SELECT i.invoice_id, i.invoice_status FROM Invoices i LEFT JOIN Payments p ON i.invoice_id = p.invoice_id WHERE p.invoice_id IS NULL

-- Soru 352: what are all shipment tracking numbers and shipment dates?
SELECT tracking_number, shipment_date FROM Shipments

-- Soru 353: What is the name of the affiliation which publishes the greatest number of papers among those whose address contains 'China'.
SELECT a.name, COUNT(DISTINCT al.paper_id) AS paper_count FROM Affiliation a JOIN Author_list al ON a.affiliation_id = al.affiliation_id WHERE a.address LIKE '%China%' GROUP BY a.name ORDER BY paper_count DESC LIMIT 1

-- Soru 354: Give the build year, model, and builder of each vehicle.
SELECT Build_Year , Model , Builder FROM vehicle

-- Soru 355: Select the warehouse codes along with the number of boxes in each warehouse.
SELECT T2.code , count(*) FROM warehouses AS T1 JOIN boxes AS T2 ON T1.code = T2.warehouse GROUP BY T2.warehouse

-- Soru 356: Which services are both used by the customer "Hardy Kutch" and are rated as "bad" in a customer interaction? Give me the service details.
SELECT T3.Service_Details FROM Customers AS T1 JOIN Customers_and_Services AS T2 ON T1.Customer_ID = T2.Customer_ID JOIN Services AS T3 ON T2.Service_ID = T3.Service_ID WHERE T1.Customer_Details = "Hardy Kutch" INTERSECT SELECT T3.Service_Details FROM Customers AS T1 JOIN Customer_Interaction AS T4 ON T1.Customer_ID = T4.Customer_ID JOIN Services AS T3 ON T4.Service_ID = T3.Service_ID WHERE T4.Rating = "bad"

-- Soru 357: Show the transmitters of radios whose ERP is bigger than 150 or smaller than 30.
SELECT Transmitter FROM radio WHERE ERP_kW > 150 OR ERP_kW < 30

-- Soru 358: Find the birth years of all distinct artists who made sculptures after 1920?
SELECT DISTINCT a.birthYear FROM Artists a JOIN Sculptures s ON a.artistID = s.artistID WHERE s.year > 1920

-- Soru 359: List the station names of city channels whose affiliation is not "ABC".
SELECT station_name FROM city_channel WHERE affiliation != 'ABC'

-- Soru 360: Show the official languages spoken by at least two countries.
SELECT l.name AS language_name, COUNT(ol.country_id) AS country_count FROM official_languages ol JOIN languages l ON ol.language_id = l.id GROUP BY l.name HAVING COUNT(ol.country_id) >= 2

-- Soru 361: List team name for all universities with enrollments above the average.
SELECT Team_Name FROM university WHERE Enrollment > (SELECT AVG(Enrollment) FROM university)

-- Soru 362: List the details for all the pairs of teachers and students who are in the same class.
SELECT s.student_details, t.teacher_details FROM Students s JOIN Classes c ON s.class_id = c.class_id JOIN Teachers t ON c.class_id = t.class_id

-- Soru 363: Give the average distance between Boston and other cities.
SELECT AVG(distance) AS avg_distance FROM City c1 JOIN Direct_distance dd ON c1.city_code = dd.city1_code OR c1.city_code = dd.city2_code WHERE (c1.city_code = 'BOS' AND dd.city2_code != 'BOS') OR (c1.city_code != 'BOS' AND dd.city1_code = 'BOS')

-- Soru 364: What is the average price for products with type Clothes?
SELECT avg(product_price) FROM products WHERE product_type_code = "Clothes"

-- Soru 365: What are the different plane names of planes with an average pilot age of below 35, and how many pilots have flown each of them?
SELECT T1.plane_name , count(*) FROM PilotSkills AS T1 JOIN Hangar AS T2 ON T1.plane_name = T2.plane_name GROUP BY T1.plane_name HAVING avg(T1.age) < 35

-- Soru 366: For each student, find the student id, student biographical data, and the number of courses he or she takes.
SELECT s.student_id, s.bio_data, COUNT(t.transcript_id) AS number_of_courses FROM Students s JOIN Transcripts t ON s.student_id = t.student_id GROUP BY s.student_id, s.bio_data

-- Soru 367: List all meeting type codes and details.
SELECT meeting_type_code, meeting_details FROM Meetings

-- Soru 368: What is the first and last name of each distinct artists who made a sculpture before 1900?
SELECT DISTINCT a.fname, a.lname FROM Artists a JOIN Sculptures s ON a.artistID = s.artistID WHERE s.year < 1900

-- Soru 369: How many different item status codes are there listed in ordered items?
SELECT COUNT(DISTINCT item_status_code) AS distinct_item_status_count FROM Orders

-- Soru 370: Find the titles of paintings and sculpture works made by the artist whose id is 222?
SELECT T1.title FROM Paintings AS T1 JOIN Artists AS T2 ON T1.painterID = T2.artistID WHERE T2.artistID = 222 UNION SELECT T3.title FROM Sculptures AS T3 JOIN Artists AS T4 ON T3.artistID = T4.artistID WHERE T4.artistID = 222

-- Soru 371: How many drivers have raced in 'NASCAR'?
SELECT count(*) FROM driver WHERE Racing_Series = 'NASCAR'

-- Soru 372: How many invoices are there for each client id?
SELECT client_id, COUNT(*) as number_of_invoices FROM Invoices GROUP BY client_id

-- Soru 373: List the types of engines that are used by at least two drivers.
SELECT Engine FROM driver GROUP BY Engine HAVING COUNT(*) >= 2

-- Soru 374: Find the names of the districts which have had both spokesman with rank position 1 and 2.
SELECT d.Name FROM district d WHERE EXISTS ( SELECT 1 FROM spokesman s1 JOIN spokesman_district sd1 ON s1.Spokesman_ID = sd1.Spokesman_ID AND sd1.District_ID = d.District_ID WHERE s1.Rank_position = 1 ) AND EXISTS ( SELECT 1 FROM spokesman s2 JOIN spokesman_district sd2 ON s2.Spokesman_ID = sd2.Spokesman_ID AND sd2.District_ID = d.District_ID WHERE s2.Rank_position = 2 )

-- Soru 375: Find the name of the oldest pilot for each type of plane, and order the results by plane name.
SELECT T1.plane_name , T2.pilot_name FROM Hangar AS T1 JOIN PilotSkills AS T2 ON T1.plane_name = T2.plane_name WHERE T2.age = ( SELECT MAX(age) FROM PilotSkills WHERE plane_name = T1.plane_name ) ORDER BY T1.plane_name

-- Soru 376: How many institutions were founded between 1850 and 1900?
SELECT count(*) FROM institution WHERE founded BETWEEN 1850 AND 1900

-- Soru 377: List the names of members whose nationality is not `` England '' .
SELECT Name FROM member WHERE Nationality != "England"

-- Soru 378: What are the names of all the authors, and how many books has each written?
SELECT Author.Name, COUNT(Book.ISBN) AS NumberOfBooks FROM Author JOIN Author_Book ON Author.idAuthor = Author_Book.Author JOIN Book ON Author_Book.ISBN = Book.ISBN GROUP BY Author.Name

-- Soru 379: How many staff do we have?
SELECT COUNT(staff_id) AS total_staff FROM Staff

-- Soru 380: Count the number of different official languages.
SELECT COUNT(DISTINCT language_id) AS number_of_official_languages FROM official_languages

-- Soru 381: List the search content of the users who do not own a single property.
SELECT u.search_content FROM Users u LEFT JOIN Properties p ON u.user_id = p.owner_id WHERE p.property_id IS NULL

-- Soru 382: Which customers do not have any order? Give me the customer names.
SELECT c.Name FROM customer c LEFT JOIN customer_order co ON c.Customer_ID = co.Customer_ID WHERE co.Dish_Name IS NULL

-- Soru 383: What are the ids and details of the students who take 2 courses?
SELECT T1.student_id, T1.bio_data, T1.student_details FROM Students AS T1 JOIN Transcripts AS T2 ON T1.student_id = T2.student_id GROUP BY T1.student_id HAVING COUNT(*) = 2

-- Soru 384: Find the name of all movies that are not played in Odeon theater.
SELECT T1.Title FROM Movies AS T1 JOIN MovieTheaters AS T2 ON T1.Code != T2.Movie WHERE T2.Name = 'Odeon'

-- Soru 385: What are the ids for male students not in the USA?
SELECT StuID FROM Student WHERE Sex = 'Male' AND city_code != (SELECT city_code FROM City WHERE country = 'USA')

-- Soru 386: What is the most frequent product type code?
SELECT product_type_code FROM Products GROUP BY product_type_code ORDER BY COUNT(*) DESC LIMIT 1

-- Soru 387: Return all platform names and corresponding market districts.
SELECT Platform_name , Market_district FROM platform

-- Soru 388: Return the different constructors of drivers, ordered by age ascending.
SELECT DISTINCT Constructor FROM driver ORDER BY Age ASC

-- Soru 389: How many customers are there?
SELECT COUNT(*) FROM customer

-- Soru 390: How much does headphones cost on average for each headphone construction?
SELECT Construction, AVG(Price) AS Average_Price FROM headphone GROUP BY Construction

-- Soru 391: What is the name of the theater playing the most movies?
SELECT T2.Name FROM MovieTheaters AS T1 JOIN Movies AS T2 ON T1.Movie = T2.Code GROUP BY T1.Name ORDER BY COUNT(*) DESC LIMIT 1

-- Soru 392: List the last names of all customers in an alphabetical order.
SELECT LastName FROM customers ORDER BY LastName

-- Soru 393: Show the transmitters of radios and the station names of the channels they are associated with in descending order of the ERP of the radios.
SELECT r.Transmitter, cc.Station_name FROM radio r JOIN city_channel cc ON r.Transmitter = cc.Station_name ORDER BY CAST(REPLACE(r.ERP_kW, ',', '') AS REAL) DESC

-- Soru 394: What are the names of clubs that have players who have won more than twice?
SELECT T1.Name FROM CLub AS T1 JOIN player AS T2 ON T1.Club_ID = T2.Club_ID WHERE T2.Wins_count > 2

-- Soru 395: How many clients are there for each sic code?
SELECT sic_code, COUNT(client_id) AS number_of_clients FROM Clients GROUP BY sic_code

-- Soru 396: What is the first name of the artist who has the highest number of sculptures?
SELECT a.fname FROM Artists a JOIN Sculptures s ON a.artistID = s.artistID GROUP BY a.artistID ORDER BY COUNT(s.sculptureID) DESC LIMIT 1

-- Soru 397: What is the average age of pilots for different types of planes?
SELECT avg(age), plane_name FROM PilotSkills GROUP BY plane_name

-- Soru 398: List all the distinct student answer texts to which comments "Normal" were given?
SELECT DISTINCT Student_Answers.Answer_Text FROM Student_Answers JOIN Comments ON Student_Answers.Answer_ID = Comments.Answer_ID WHERE Comments.Comment_Text = 'Normal'

-- Soru 399: Show the distinct transmitters of radios that are not associated with any city channel.
SELECT DISTINCT r.Transmitter FROM radio r LEFT JOIN city_channel cc ON r.Transmitter = cc.Station_name WHERE cc.Station_name IS NULL

-- Soru 400: Find all information of on pilots whose age is less than 30.
SELECT * FROM PilotSkills WHERE age < 30

-- Soru 401: What are the names and powertrain types of cars that have more than 30 total rental hours?
SELECT name, type_of_powertrain FROM Vehicles WHERE id IN ( SELECT vehicle_id FROM Rental_Hours GROUP BY vehicle_id HAVING SUM(hours_rented) > 30 )

-- Soru 402: What are the ids for papers with titles containing 'translation'?
SELECT paper_id FROM Paper WHERE title LIKE '%translation%'

-- Soru 403: What is the maximum, average, and minimum enrollment for universities?
SELECT MAX(Enrollment), AVG(Enrollment), MIN(Enrollment) FROM university

-- Soru 404: Show agency details for client with detail 'Mac'.
SELECT Agencies.agency_details FROM Clients JOIN Agencies ON Clients.agency_id = Agencies.agency_id WHERE Clients.client_details = 'Mac'

-- Soru 405: For the document subset with the most number of different documents , what are the ids and names of the subset , as well as the number of documents ?
SELECT ds.Document_Subset_ID, ds.Document_Subset_Name, COUNT(DISTINCT do.Document_Object_ID) AS Number_of_Documents FROM Document_Subsets ds JOIN Document_Objects do ON ds.Document_Subset_ID = do.Parent_Document_Object_ID GROUP BY ds.Document_Subset_ID, ds.Document_Subset_Name ORDER BY Number_of_Documents DESC LIMIT 1

-- Soru 406: What is the name of the client who received the heaviest package?
SELECT C.Name FROM Client C JOIN Package P ON C.ClientID = P.ClientID ORDER BY P.Weight DESC LIMIT 1

-- Soru 407: Find the details of the teachers who have taught the student with the earliest transcript issuance.
SELECT T3.teacher_details FROM Students AS T1 JOIN Transcripts AS T2 ON T1.student_id = T2.student_id JOIN Student_Teachers AS T4 ON T1.student_id = T4.student_id JOIN Teachers AS T3 ON T4.teacher_id = T3.teacher_id ORDER BY T2.date_of_transcript ASC LIMIT 1

-- Soru 408: What are the average rhythm scores for the songs in each different language?
SELECT s.language, AVG(ps.rhythm_tempo) AS average_rhythm_score FROM performance_score ps JOIN songs s ON ps.songs_id = s.id GROUP BY s.language

-- Soru 409: How many orders do we have for "Pride and Prejudice"?
SELECT COUNT(o.IdOrder) FROM Orders o JOIN Book b ON o.ISBN = b.ISBN WHERE b.Title = 'Pride and Prejudice'

-- Soru 410: Show the other details for the author Addison Denesik.
SELECT other_details FROM Authors WHERE author_name = 'Addison Denesik'

-- Soru 411: What is the average units sold in millions of the games that are not developed by Nintendo?
SELECT AVG(Units_sold_Millions) AS Average_Units_Sold FROM game WHERE Developers != 'Nintendo'

-- Soru 412: Find the total value of boxes in the warehouses located at Chicago or New York.
SELECT sum(T2.Value) FROM Warehouses AS T1 JOIN Boxes AS T2 ON T1.Code = T2.Warehouse WHERE T1.Location = 'Chicago' OR T1.Location = 'New York'

-- Soru 413: Count the number of pilots with age greater than 40.
SELECT count(*) FROM Pilotskills WHERE age > 40

-- Soru 414: When did the user with login name ratione register?
SELECT registration_date FROM Users WHERE login_name = 'ratione'

-- Soru 415: Find the nationality and card credit of each customer.
SELECT Nationality , Card_Credit FROM customer

-- Soru 416: Find the unique id of the painters who had medium oil paintings exhibited at gallery 240?
SELECT DISTINCT painterID FROM Paintings WHERE location = 'gallery 240' AND medium = 'oil'

-- Soru 417: what are the average and maximum profit of a year for all presses?
SELECT AVG(Year_Profits_billion), MAX(Year_Profits_billion) FROM press

-- Soru 418: What are the names of races in which drivers 26 or older took part?
SELECT T2.Race_Name FROM driver AS T1 JOIN race AS T2 ON T1.Driver_ID = T2.Driver_ID WHERE T1.Age >= 26

-- Soru 419: Show the ids and details for all staff.
SELECT staff_id, staff_details FROM Staff

-- Soru 420: Show names of players and names of clubs they are in.
SELECT player.Name AS Player_Name, club.Name AS Club_Name FROM player JOIN club ON player.Club_ID = club.Club_ID

-- Soru 421: Give the average justice scores across all countries.
SELECT AVG(justice_score) AS average_justice_score FROM countries

-- Soru 422: List the nicknames of institutions in descending order of capacity.
SELECT T2.Nickname FROM institution AS T1 JOIN championship AS T2 ON T1.Institution_ID = T2.Institution_ID ORDER BY T1.Capacity DESC

-- Soru 423: Find all locations of planes sorted by the plane name.
SELECT T2.location FROM PilotSkills AS T1 JOIN Hangar AS T2 ON T1.plane_name = T2.plane_name ORDER BY T1.plane_name

-- Soru 424: What are the type and title of the books that are not rated?
SELECT TYPE , Title FROM book WHERE Book_ID NOT IN (SELECT Book_ID FROM review)

-- Soru 425: How many languages are there?
SELECT COUNT(*) FROM languages

-- Soru 426: For each student answer, find the email address of the student and the date of the answer. Sort them in descending order of dates.
SELECT Students.Email_Adress, Answers.Answer_Date FROM Students JOIN Answers ON Students.Student_ID = Answers.Student_ID ORDER BY Answers.Answer_Date DESC

-- Soru 427: Show all payment method codes and the number of customers in each code.
SELECT payment_method_code, COUNT(customer_id) AS number_of_customers FROM Customers GROUP BY payment_method_code

-- Soru 428: Show the book title and purchase price of the book that has had the greatest amount in orders.
SELECT T2.Title, SUM(T2.PurchasePrice) as TotalPurchasePrice FROM Orders AS T1 JOIN Book AS T2 ON T1.ISBN = T2.ISBN GROUP BY T2.ISBN ORDER BY TotalPurchasePrice DESC LIMIT 1

-- Soru 429: What are all the address ids and address details?
SELECT address_id, address_details FROM Addresses

-- Soru 430: List the last names of the students whose gender is not "M".
SELECT Last_Name FROM Students WHERE Gender_MFU != 'M'

-- Soru 431: Count the number of goods for each food type.
SELECT Food , COUNT(*) FROM goods GROUP BY Food

-- Soru 432: List the codes and descriptions for all process status.
SELECT process_status_code, process_status_description FROM Process_Status

-- Soru 433: What is the list of building names, sorted by the number of stories of each building in ascending order?
SELECT name FROM building ORDER BY Number_of_Stories ASC

-- Soru 434: Show all client names for clients who have not made orders.
SELECT Name FROM Client WHERE IdClient NOT IN (SELECT IdClient FROM Orders)

-- Soru 435: What is the title of the book written by Plato has price lower than the average sale price of all books?
SELECT Title FROM Book WHERE Author = 'Plato' AND SalePrice < (SELECT AVG(SalePrice) FROM Book)

-- Soru 436: What are the first and last names of the artists who have not only medium oil paintings but also paintings with the lithographic medium?
SELECT T1.fname, T1.lname FROM Artists AS T1 JOIN Paintings AS T2 ON T1.artistID = T2.painterID WHERE T2.medium = 'oil' INTERSECT SELECT T1.fname, T1.lname FROM Artists AS T1 JOIN Paintings AS T2 ON T1.artistID = T2.painterID WHERE T2.medium = 'lithographic'

-- Soru 437: Show the vehicle name with a descending order of total hours of renting.
SELECT Vehicles.name, SUM(Rentals.hours_rented) AS total_hours_rented FROM Vehicles JOIN Rentals ON Vehicles.id = Rentals.vehicle_id GROUP BY Vehicles.name ORDER BY total_hours_rented DESC

-- Soru 438: What are the last names and ids of users who have searched two or fewer times, and own two or more properties?
SELECT u.last_name, u.user_id FROM Users u JOIN User_Searches us ON u.user_id = us.user_id JOIN Properties p ON u.user_id = p.owner_id GROUP BY u.user_id, u.last_name HAVING COUNT(us.search_id) <= 2 AND COUNT(p.property_id) >= 2

-- Soru 439: For each document object id, how many children do they have?
SELECT d1.Document_Object_ID, COUNT(d2.Document_Object_ID) AS Number_of_Children FROM Document_Objects d1 LEFT JOIN Document_Objects d2 ON d1.Document_Object_ID = d2.Parent_Document_Object_ID GROUP BY d1.Document_Object_ID

-- Soru 440: What are the names, ages, and membership credits for all customers?
SELECT name , age , membership_credit FROM Customers

-- Soru 441: What is the lowest room count across all the properties?
SELECT MIN(room_count) AS lowest_room_count FROM Properties

-- Soru 442: What are the id and height of the painting with the longest width in gallery 240?
SELECT paintingID, height_mm FROM Paintings WHERE gallery = 240 ORDER BY width_mm DESC LIMIT 1

-- Soru 443: What is the title of the book that has been ordered the greatest number of times?
SELECT b.Title, COUNT(o.IdOrder) as TotalOrders FROM Orders o JOIN Client c ON o.IdClient = c.IdClient JOIN Book b ON o.ISBN = b.ISBN GROUP BY b.Title ORDER BY TotalOrders DESC LIMIT 1

-- Soru 444: Find the id of the papers whose title has the key word 'translation'.
SELECT paper_id FROM Paper WHERE title LIKE '%translation%'

-- Soru 445: Count the number of authors of age below 30.
SELECT COUNT(*) FROM author WHERE Age < 30

-- Soru 446: Show the number of staff roles.
SELECT COUNT(staff_role_code) AS number_of_staff_roles FROM Ref_Staff_Roles

-- Soru 447: What is the average price of the products being ordered?
SELECT AVG(product_price) AS average_price FROM Products WHERE is_ordered = 1

-- Soru 448: What are the name and citizenship of the drivers who have driven the vehicle model 'DJ1'?
SELECT T2.Name , T2.Citizenship FROM vehicle_driver AS T1 JOIN driver AS T2 ON T1.Driver_ID = T2.Driver_ID JOIN vehicle AS T3 ON T1.Vehicle_ID = T3.Vehicle_ID WHERE T3.Model = 'DJ1'

-- Soru 449: Tell me the height and id number of the widest painting in gallery 240.
SELECT height_mm, paintingID FROM Paintings WHERE location = 'gallery 240' ORDER BY width_mm DESC LIMIT 1

-- Soru 450: What is the smallest number of packages received and by whom ?
SELECT Manager, COUNT(*) AS NumberOfPackages FROM Shipment GROUP BY Manager ORDER BY NumberOfPackages ASC LIMIT 1

-- Soru 451: Show all staff role codes and the number of document processes for each role.
SELECT r.staff_role_code, COUNT(dp.process_id) AS number_of_document_processes FROM Ref_Staff_Roles r LEFT JOIN Document_Processes dp ON r.staff_role_code = dp.role_code GROUP BY r.staff_role_code

-- Soru 452: How many kinds of products have not been sold?
SELECT COUNT(*) AS unsold_products_count FROM Products p LEFT JOIN Sales s ON p.product_id = s.product_id WHERE s.product_id IS NULL

-- Soru 453: What are the names of clubs who do not have the manufacturer Nike?
SELECT Name FROM CLub WHERE Manufacturer != 'Nike'

-- Soru 454: How many child documents does each parent document has? List the document id and the number.
SELECT Parent_Document_Object_ID AS Document_ID, COUNT(Document_Object_ID) AS Number_of_Children FROM Document_Objects GROUP BY Parent_Document_Object_ID

-- Soru 455: Find all the items that have chocolate flavor but were not bought more than 10 times.
SELECT g.Id, g.Flavor, g.Food FROM goods g JOIN items i ON g.Id = i.Item WHERE g.Flavor = 'chocolate' GROUP BY g.Id, g.Flavor, g.Food HAVING COUNT(i.Receipt) <= 10

-- Soru 456: What is the product type code with most number of products?
SELECT product_type_code FROM Products GROUP BY product_type_code ORDER BY COUNT(*) DESC LIMIT 1

-- Soru 457: Find the top 3 artists who have the biggest number of painting works whose medium is oil?
SELECT a.fname, a.lname, COUNT(p.paintingID) AS num_paintings FROM Artists a JOIN Paintings p ON a.artistID = p.painterID WHERE p.medium = 'oil' GROUP BY a.artistID ORDER BY num_paintings DESC LIMIT 3

-- Soru 458: What are all isbns for each book, and how many times has each been ordered?
SELECT B.ISBN, COUNT(O.IdOrder) as OrderCount FROM Orders O JOIN Book B ON O.ISBN = B.ISBN GROUP BY B.ISBN

-- Soru 459: What are the 5 best books in terms of sale amount? Give me their titles and release dates.
SELECT Title, Release_date FROM book ORDER BY CAST(REPLACE(Sale_Amount, ',', '') AS REAL) DESC LIMIT 5

-- Soru 460: Which item was bought the fewest times?
SELECT Item, COUNT(*) AS Count FROM items GROUP BY Item ORDER BY Count ASC LIMIT 1

-- Soru 461: What is the name, rating, and age for every sailor? And order them by rating and age.
SELECT name, rating, age FROM Sailors ORDER BY rating, age

-- Soru 462: How many planes are controlled by the pilots whose age is older than 40?
SELECT count(*) FROM pilotskills WHERE age > 40

-- Soru 463: What are the buildings, streets, and cities corresponding to the addresses of senior citizens?
SELECT b.building_name, s.street_name, c.city_name FROM Addresses a JOIN Buildings b ON a.address_id = b.address_id JOIN Streets s ON a.address_id = s.address_id JOIN Cities c ON a.address_id = c.address_id JOIN Users u ON a.user_id = u.user_id WHERE u.age_category_code IN (SELECT age_category_code FROM Ref_Age_Categories WHERE age_category_description LIKE '%senior%')

-- Soru 464: What is the name of the sailors who reserved boat with id 103?
SELECT T1.name FROM sailors AS T1 JOIN reserves AS T2 ON T1.sid = T2.sid WHERE T2.bid = 103

-- Soru 465: Return the average and minimum ages across all pilots.
SELECT avg(age) , min(age) FROM PilotSkills

-- Soru 466: Show all information of all unrated movies.
SELECT * FROM Movies WHERE Rating = 'null'

-- Soru 467: What are the names of the press that makes the highest monthly profit or the lowest monthly profit?
SELECT Name FROM press ORDER BY Month_Profits_billion DESC LIMIT 1 UNION SELECT Name FROM press ORDER BY Month_Profits_billion ASC LIMIT 1

-- Soru 468: Who receieved the heaviest package?
SELECT e.Name AS EmployeeName, p.Weight AS PackageWeight FROM Employee e JOIN Shipment s ON e.EmployeeID = s.Manager JOIN Package p ON s.ShipmentID = p.ShipmentID ORDER BY p.Weight DESC LIMIT 1

-- Soru 469: On average, how many rooms do properties with garden features have?
SELECT AVG(num_rooms) FROM ( SELECT p.property_id, COUNT(r.room_type_code) AS num_rooms FROM Properties p JOIN Property_Rooms pr ON p.property_id = pr.property_id JOIN Rooms r ON pr.room_id = r.room_id WHERE EXISTS ( SELECT 1 FROM Features f JOIN Property_Features pf ON f.feature_id = pf.feature_id WHERE pf.property_id = p.property_id AND f.feature_description = 'garden' ) GROUP BY p.property_id ) AS room_counts

-- Soru 470: How many products named Monitor have been ordered?
SELECT count(*) FROM products WHERE product_name = "Monitor"

-- Soru 471: What is the name of the most popular plane?
SELECT T1.plane_name FROM Hangar AS T1 JOIN PilotSkills AS T2 ON T1.plane_name = T2.plane_name GROUP BY T1.plane_name ORDER BY count(*) DESC LIMIT 1

-- Soru 472: What are the different names, prices, and descriptions for all products bought by female customers?
SELECT DISTINCT T1.product_name , T1.product_price , T1.product_description FROM Products AS T1 JOIN Orders AS T2 ON T1.product_id = T2.product_id JOIN Customers AS T3 ON T2.customer_id = T3.customer_id WHERE T3.gender_code = "F"

-- Soru 473: Find the total capacity of all warehouse locations.
SELECT sum(Capacity) FROM Warehouses

-- Soru 474: Find the name of the district which has greatest number of spokesmen.
SELECT d.Name FROM district d JOIN spokesman_district sd ON d.District_ID = sd.District_ID GROUP BY d.District_ID ORDER BY COUNT(sd.Spokesman_ID) DESC LIMIT 1

-- Soru 475: Find the names of all pilots with age between 30 and 40 sorted by their ages in ascending order.
SELECT pilot_name FROM PilotSkills WHERE age BETWEEN 30 AND 40 ORDER BY age ASC

-- Soru 476: Show all product names without an order.
SELECT p.product_name FROM Products p LEFT JOIN Customer_Orders co ON p.product_id = co.product_id WHERE co.order_id IS NULL

-- Soru 477: What are the names of all sailors with a higher rating than every sailor named Luis?
SELECT name FROM sailors WHERE rating > (SELECT max(rating) FROM sailors WHERE name = 'Luis')

-- Soru 478: List all product type codes and the number of products in each type.
SELECT product_type_code, COUNT(*) AS number_of_products FROM Products GROUP BY product_type_code

-- Soru 479: What are the usernames and passwords of all customers whose phone number starts with '+12'?
SELECT login_name, login_password FROM Customers WHERE phone_number LIKE '+12%'

-- Soru 480: Which nations have both customers with card credit above 50 and customers with card credit below 75.
SELECT Nationality FROM customer WHERE Card_Credit > 50 INTERSECT SELECT Nationality FROM customer WHERE Card_Credit < 75

-- Soru 481: What are the id, language and original artist of the songs whose name is not 'Love'?
SELECT id , language , original_artist FROM songs WHERE name != 'Love'

-- Soru 482: Which team does not have drivers?
SELECT t.Team_ID, t.Team FROM team t LEFT JOIN driver d ON t.Car_Owner = d.Driver_ID WHERE d.Driver_ID IS NULL

-- Soru 483: What are the names and codes for all majors ordered by their code?
SELECT Major_Name , Major_Code FROM major ORDER BY Major_Code

-- Soru 484: What are the different document object ids that are in the collection named Best but not in the subset named 'Best for 2000'?
SELECT DISTINCT d.Document_Object_ID FROM Document_Objects d JOIN Collection_Subsets c ON d.Parent_Document_Object_ID = c.Collection_Subset_ID WHERE c.Collection_Subset_Name = 'Best' AND d.Document_Object_ID NOT IN ( SELECT Document_Object_ID FROM Document_Objects JOIN Collection_Subsets ON Parent_Document_Object_ID = Collection_Subset_ID WHERE Collection_Subset_Name = 'Best for 2000' )

-- Soru 485: What is the name and model year of the vehicle which has been rented the most times?
SELECT V.name, V.Model_year FROM Vehicles AS V JOIN Rentals AS R ON V.id = R.vehicle_id GROUP BY V.id ORDER BY COUNT(R.id) DESC LIMIT 1

-- Soru 486: How many business processes do we have?
SELECT COUNT(*) FROM Processes

-- Soru 487: Show all invoice status codes and the number of invoices with each status.
SELECT invoice_status, COUNT(*) AS number_of_invoices FROM Invoices GROUP BY invoice_status

-- Soru 488: What are the names of the countries, ordered descending by overall score?
SELECT name FROM countries ORDER BY overall_score DESC

-- Soru 489: For each student answer, find the first name of the student and the date of the answer.
SELECT S.First_Name, A.Answer_Date FROM Students S JOIN Answers A ON S.Student_ID = A.Student_ID

-- Soru 490: What is the number of documents in the collection named 'Best'?
SELECT count(*) FROM Collection_Subsets WHERE Collection_Subset_Name = "Best"

-- Soru 491: What are the average laps of all the drivers who are younger than 20?
SELECT AVG(Laps) AS Average_Laps FROM driver WHERE Age < 20

-- Soru 492: What are the names of pilots who own both Piper Cub and the B-52 Bomber?
SELECT T1.pilot_name FROM Pilotskills AS T1 JOIN Hangar AS T2 ON T1.plane_name = T2.plane_name WHERE T2.location = 'Piper Cub' INTERSECT SELECT T1.pilot_name FROM Pilotskills AS T1 JOIN Hangar AS T2 ON T1.plane_name = T2.plane_name WHERE T2.location = 'B-52 Bomber'

-- Soru 493: How many movies exist for each rating?
SELECT count(*) , rating FROM movies GROUP BY rating

-- Soru 494: What is the id of the artist with the most paintings before 1900?
SELECT painterID FROM Paintings WHERE year < 1900 GROUP BY painterID ORDER BY COUNT(*) DESC LIMIT 1

-- Soru 495: Which date corresponds to when a customer purchased a good costing over 15 dollars?
SELECT T2.Date FROM items AS T1 JOIN receipts AS T2 ON T1.Receipt = T2.ReceiptNumber WHERE T1.Item IN ( SELECT Id FROM goods WHERE Price > 15 )

-- Soru 496: Count the number of buildings.
SELECT COUNT(*) FROM building

-- Soru 497: How many movies have a rating that is not null?
SELECT count(*) FROM Movies WHERE Rating != "null"

-- Soru 498: What are the names of the 5 largest regions in terms of area?
SELECT name FROM region ORDER BY area DESC LIMIT 5

-- Soru 499: Find the manager and sponsor of the team that has the most drivers.
SELECT T1.Manager, T1.Sponsor FROM team AS T1 JOIN driver AS T2 ON T1.Team_ID = T2.Car_# GROUP BY T1.Team_ID ORDER BY COUNT(T2.Driver_ID) DESC LIMIT 1

-- Soru 500: Which services were used by the customer with details "Hardy Kutch"? Give me the service details.
SELECT T3.Service_Details FROM Customers AS T1 JOIN Customers_and_Services AS T2 ON T1.Customer_ID = T2.Customer_ID JOIN Services AS T3 ON T2.Service_ID = T3.Service_ID WHERE T1.Customer_Details = "Hardy Kutch"

