<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');
$stmt = $pdo->query('SELECT * FROM franchises');
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
$stmt = $pdo->query('SELECT * FROM utilisateur');
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
