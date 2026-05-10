<?php
$conn = new mysqli("localhost", "root", "", "boussole");
if ($conn->connect_error) { die("Connection failed: " . $conn->connect_error); }
$res = $conn->query("SELECT * FROM budget_previsionnel");
while ($row = $res->fetch_assoc()) {
    print_r($row);
}
