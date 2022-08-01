import dev.efnilite.vilib.vector.Vector3D;

public class Testing {

    public static void main(String[] args) {
        System.out.println(angle(new Vector3D(1, 0, 0), new Vector3D(1, 0, 1)));
    }

    public static double angle(Vector3D base, Vector3D other) {
        double dotProduct = base.x * other.x + base.y * other.y + base.z * other.z;
        double divideBy = base.length() * other.length();

        return Math.toDegrees(Math.acos(dotProduct / divideBy));
    }
}
