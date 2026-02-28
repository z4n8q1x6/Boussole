import cv2
from deepface import DeepFace
import sys
import os
import time

def find_user_image(user_email):
    """
    Trouve le chemin de l'image pour un utilisateur donné.
    Les images doivent être stockées dans 'python_scripts/user_images/'.
    Le nom du fichier doit être 'email.jpg' (ou .png).
    """
    base_path = os.path.join(os.path.dirname(__file__), 'user_images')

    if not os.path.exists(base_path):
        os.makedirs(base_path)
        print(f"INFO: Dossier 'user_images' créé. Placez l'image de référence pour {user_email} ici.")
        return None

    for ext in ['.jpg', '.jpeg', '.png']:
        user_image_path = os.path.join(base_path, user_email + ext)
        if os.path.exists(user_image_path):
            return user_image_path

    print(f"INFO: Image pour {user_email} non trouvée dans 'user_images'.")
    return None

def main(user_email):
    """
    Fonction principale pour la reconnaissance faciale avec DeepFace.
    """
    user_image_path = find_user_image(user_email)

    if not user_image_path:
        print("AUTH_FAILED: Image de référence introuvable.")
        sys.exit(1)

    # Initialiser la webcam
    video_capture = cv2.VideoCapture(0)
    if not video_capture.isOpened():
        print("AUTH_FAILED: Impossible d'accéder à la webcam.")
        sys.exit(1)

    print("INFO: Webcam initialisée. Regardez la caméra...")

    start_time = time.time()
    timeout = 7  # Tenter la reconnaissance pendant 7 secondes

    try:
        while time.time() - start_time < timeout:
            ret, frame = video_capture.read()
            if not ret:
                break

            # DeepFace.verify fait tout le travail : détection, alignement, comparaison.
            # 'VGG-Face' est un modèle rapide et fiable. 'distance_metric' peut être 'cosine', 'euclidean', etc.
            # 'enforce_detection=False' évite que le programme ne crashe si aucun visage n'est détecté.
            try:
                result = DeepFace.verify(
                    img1_path=frame,
                    img2_path=user_image_path,
                    model_name="VGG-Face",
                    detector_backend="opencv",
                    enforce_detection=False
                )

                # 'result' est un dictionnaire. La clé 'verified' est un booléen.
                if result.get("verified", False):
                    print("AUTH_SUCCESS")
                    video_capture.release()
                    cv2.destroyAllWindows()
                    sys.exit(0)

            except Exception as e:
                # DeepFace peut lever des exceptions si aucun visage n'est trouvé dans le frame.
                # On les ignore pour continuer à essayer.
                pass

            # Optionnel: Afficher une fenêtre pour le débogage
            # cv2.imshow('Video', frame)
            # if cv2.waitKey(1) & 0xFF == ord('q'):
            #     break

    finally:
        # S'assurer que la webcam est bien libérée
        video_capture.release()
        cv2.destroyAllWindows()

    print("AUTH_FAILED: Visage non reconnu dans le temps imparti.")
    sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        user_email = sys.argv[1]
        main(user_email)
    else:
        print("AUTH_FAILED: Email de l'utilisateur manquant.")
        sys.exit(1)
