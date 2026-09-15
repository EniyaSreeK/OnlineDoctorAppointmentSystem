<!DOCTYPE html>
<html>
<head>
    <title>Book Appointment</title>
</head>
<body>

    <h1>Book Doctor Appointment</h1>

    <form action="appointment" method="post">

        <label>Patient Name:</label>
        <input type="text" name="patientName">

        <br><br>

        <label>Doctor Name:</label>
        <input type="text" name="doctorName">

        <br><br>

        <label>Date:</label>
        <input type="date" name="date">

        <br><br>

        <input type="submit" value="Book Appointment">

    </form>

</body>
</html>