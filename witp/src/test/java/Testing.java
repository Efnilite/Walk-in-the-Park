import dev.efnilite.vilib.vector.Vector3D;

public class Testing {

    public static void main(String[] args) {
        System.out.println(angle(new Vector3D(1, 0, 0), new Vector3D(0, 0, 1)));
        System.out.println(angle(new Vector3D(1, 0, 0), new Vector3D(1, 0, 0)));
        System.out.println(angle(new Vector3D(1, 0, 0), new Vector3D(-1, 0, 0)));
        System.out.println(angle(new Vector3D(1, 0, 0), new Vector3D(-1, 0, -1)));
        System.out.println(angle(new Vector3D(1, 0, 0), new Vector3D(-1, 0, -1)));
    }

    public static double angle(Vector3D base, Vector3D other) {
        return Math.toDegrees(Math.atan2(base.z, base.x) - Math.atan2(other.z, other.x));
    }
}
