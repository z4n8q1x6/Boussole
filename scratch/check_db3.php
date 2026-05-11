<?php
$conn = new mysqli("localhost", "root", "", "boussole");
if ($conn->connect_error) { die("Connection failed: " . $conn->connect_error); }
$res = $conn->query("SELECT * FROM bilan");
while ($row = $res->fetch_assoc()) {
    print_r($row);
}
