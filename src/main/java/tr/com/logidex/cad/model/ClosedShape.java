package tr.com.logidex.cad.model;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.CalcUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class ClosedShape {
    private Lbl lbl;
    private List<Line> lines = new ArrayList<Line>();
    private double minX, maxX, minY, maxY;
    private Point2D center;
    private BoundingBox bounds;
    private boolean calculatedCenterPointIsInThisShape;
    private Color color = Color.BLUE;
    private Color originalColor; // unchangeable
    private boolean shapeSelected;
    private boolean shapePrinted;
    private Integer ID;
    private final boolean isGGTFile;

    public ClosedShape(List<Line> lines,boolean isGGT) {
        this.lines = lines;
        this.isGGTFile = isGGT;
        Random random = new Random();
        Color fillColor = Color.rgb(
                random.nextInt(256),
                random.nextInt(100),
                 random.nextInt(256),
                0.5);
        this.color = fillColor;
        originalColor = this.color;
        analysePath(lines);
    }

    /**
     * Used to perform the Raycasting Algorithm to find out whether a point is in a
     * given polygon.
     * https://www.algorithms-and-technologies.com/point_in_polygon/java Performs
     * the even-odd-rule Algorithm to find out whether a point is in a given
     * polygon. This runs in O(n) where n is the number of edges of the polygon.
     *
     * @param lines an array representation of the polygon where polygon[i][0] is
     *              the x Value of the i-th point and polygon[i][1] is the y
     *              Value.
     * @param p     an array representation of the point where point[0] is its x
     *              Value and point[1] is its y Value
     * @return whether the point is in the polygon (not on the edge, just turn <
     * into <= and > into >= for that)
     */
    public static boolean pointInPolygon(List<Line> lines, Point2D p) {
        double[][] polygon = new double[lines.size()][2];
        for (Line line : lines) {
            polygon[lines.indexOf(line)][0] = line.getStartX();
            polygon[lines.indexOf(line)][1] = line.getStartY();
        }
        double[] point = new double[2];
        point[0] = p.getX();
        point[1] = p.getY();
        // A point is in a polygon if a line from the point to infinity crosses the
        // polygon an odd number of times
        boolean odd = false;
        // int totalCrosses = 0; // this is just used for debugging
        // For each edge (In this case for each point of the polygon and the previous
        // one)
        for (int i = 0, j = polygon.length - 1; i < polygon.length; i++) { // Starting with the edge from the last to the first node
            // If a line from the point into infinity crosses this edge
            if (((polygon[i][1] > point[1]) != (polygon[j][1] > point[1])) // One point needs to be above, one below our y coordinate
                    // ...and the edge doesn't cross our Y corrdinate before our x coordinate (but
                    // between our x coordinate and infinity)
                    && (point[0] < (polygon[j][0] - polygon[i][0]) * (point[1] - polygon[i][1]) / (polygon[j][1] - polygon[i][1]) + polygon[i][0])) {
                // Invert odd
                // System.out.println("Point crosses edge " + (j + 1));
                // totalCrosses++;
                odd = !odd;
            }
            // else {System.out.println("Point does not cross edge " + (j + 1));}
            j = i;
        }
        // System.out.println("Total number of crossings: " + totalCrosses);
        // If the number of crossings was odd, the point is in the polygon
        return odd;
    }


    /**
     * https://stackoverflow.com/questions/19766485/how-to-calculate-centroid-of-polygon-in-c
     *
     * @param lines
     * @return
     */
    public static Point2D calculateCentroid(List<Line> lines) {
        ArrayList<Double> xP = new ArrayList<Double>();
        ArrayList<Double> yP = new ArrayList<Double>();
        lines.forEach(line -> {
            double x1 = line.getStartX();
            double y1 = line.getStartY();
            double x2 = line.getEndX();
            double y2 = line.getEndY();
            xP.add(x2);
            xP.add(x1);
            yP.add(y2);
            yP.add(y1);
        });
        // 9004.825,9055.1,9069.65,9083.925000000001,9091.025,9097.800000000001,9110.65,9116.725,9122.475,9129.775,9136.75,9143.800000000001,9150.425000000001,9154.025,9157.275,9160.275,9162.975,9165.35,9167.4,9169.175000000001,9170.6,9172.2,9173.25,9170.300000000001,9173.25,9172.2,9170.6,9169.175000000001,9167.4,9165.35,9162.975,9160.275,9157.275,9154.0,9150.425000000001,9143.800000000001,9136.75,9129.775,9122.5,9113.575,9105.875,9097.825,9091.050000000001,9083.925000000001,9069.45,9059.7,9055.1,9004.825,8950.675000000001,9000.925000000001,9006.45,9017.25,9027.475,9040.4,9046.6,9052.525,9058.325,9065.9,9073.125,9078.025,9081.775,9089.2,9095.75,9101.025,9105.175000000001,9108.050000000001,9109.975,9110.625,9113.575,9110.625,9109.975,9108.075,9105.2,9101.075,9095.800000000001,9089.275,9081.85,9076.85,9073.2,9061.575,9052.575,9046.675000000001,9040.475,9027.5,9016.975,9000.925000000001,8950.625
        // 1072.925,982.7750000000001,955.85,928.6,913.7,898.6750000000001,867.725,852.1750000000001,836.5,814.1500000000001,791.725,766.375,740.9250000000001,724.7750000000001,708.5500000000001,692.2750000000001,675.95,659.6,643.2,626.7750000000001,610.3000000000001,579.65,548.975,548.975,548.975,518.325,487.675,471.225,454.77500000000003,438.375,422.02500000000003,405.70000000000005,389.425,373.20000000000005,357.05,331.6,306.25,283.825,261.52500000000003,238.0,218.60000000000002,199.35000000000002,184.275,169.375,141.775,123.7,115.2,25.05,57.5,147.6,157.775,177.675,197.45000000000002,225.15,239.125,253.20000000000002,268.90000000000003,290.15000000000003,311.475,327.625,341.02500000000003,370.925,401.0,431.3,461.77500000000003,492.40000000000003,523.075,548.975,548.975,548.975,574.875,605.525,636.1,666.5500000000001,696.825,726.875,756.725,774.35,786.3000000000001,820.325,844.625,858.725,872.725,900.4250000000001,920.6750000000001,950.4000000000001,1040.45
        // double x[] = new double[]{
        // 9004.825,9055.1,9069.65,9083.925000000001,9091.025,9097.800000000001,9110.65,9116.725,9122.475,9129.775,9136.75,9143.800000000001,9150.425000000001,9154.025,9157.275,9160.275,9162.975,9165.35,9167.4,9169.175000000001,9170.6,9172.2,9173.25,9170.300000000001,9173.25,9172.2,9170.6,9169.175000000001,9167.4,9165.35,9162.975,9160.275,9157.275,9154.0,9150.425000000001,9143.800000000001,9136.75,9129.775,9122.5,9113.575,9105.875,9097.825,9091.050000000001,9083.925000000001,9069.45,9059.7,9055.1,9004.825,8950.675000000001,9000.925000000001,9006.45,9017.25,9027.475,9040.4,9046.6,9052.525,9058.325,9065.9,9073.125,9078.025,9081.775,9089.2,9095.75,9101.025,9105.175000000001,9108.050000000001,9109.975,9110.625,9113.575,9110.625,9109.975,9108.075,9105.2,9101.075,9095.800000000001,9089.275,9081.85,9076.85,9073.2,9061.575,9052.575,9046.675000000001,9040.475,9027.5,9016.975,9000.925000000001,8950.625};
        // double y[] = new double[]{
        // 1072.925,982.7750000000001,955.85,928.6,913.7,898.6750000000001,867.725,852.1750000000001,836.5,814.1500000000001,791.725,766.375,740.9250000000001,724.7750000000001,708.5500000000001,692.2750000000001,675.95,659.6,643.2,626.7750000000001,610.3000000000001,579.65,548.975,548.975,548.975,518.325,487.675,471.225,454.77500000000003,438.375,422.02500000000003,405.70000000000005,389.425,373.20000000000005,357.05,331.6,306.25,283.825,261.52500000000003,238.0,218.60000000000002,199.35000000000002,184.275,169.375,141.775,123.7,115.2,25.05,57.5,147.6,157.775,177.675,197.45000000000002,225.15,239.125,253.20000000000002,268.90000000000003,290.15000000000003,311.475,327.625,341.02500000000003,370.925,401.0,431.3,461.77500000000003,492.40000000000003,523.075,548.975,548.975,548.975,574.875,605.525,636.1,666.5500000000001,696.825,726.875,756.725,774.35,786.3000000000001,820.325,844.625,858.725,872.725,900.4250000000001,920.6750000000001,950.4000000000001,1040.45
        // };
        double[] x = CalcUtils.convertDoubles(xP);
        double[] y = CalcUtils.convertDoubles(yP);
        int n = x.length;
        double a, cx, cy, t;
        int i, i1;
        /* First calculate the polygon's signed area A */
        a = 0.0f;
        i1 = 1;
        for (i = 0; i < n; i++) {
            a += x[i] * y[i1] - x[i1] * y[i];
            i1 = (i1 + 1) % n;
        }
        a *= 0.5;
//		System.out.println(a);
        // System.out.println(Math.abs(a));
        /* Now calculate the centroid coordinates Cx and Cy */
        cx = cy = 0.0f;
        i1 = 1;
        for (i = 0; i < n; i++) {
            t = x[i] * y[i1] - x[i1] * y[i];
            cx += (x[i] + x[i1]) * t;
            cy += (y[i] + y[i1]) * t;
            i1 = (i1 + 1) % n;
        }
        cx = cx / (6.0f * a);
        cy = cy / (6.0f * a);
        // System.out.println(cx);
        // System.out.println(cy);
        return new Point2D(cx, cy);
    }


    public static Point2D calculateCentroidGGT(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return new Point2D(0, 0);
        }

        // Önce benzersiz noktaları topla ve sıralı bir polygon oluştur
        List<Point2D> orderedPoints = new ArrayList<>();

        // İlk noktayı ekle
        orderedPoints.add(new Point2D(lines.get(0).getStartX(), lines.get(0).getStartY()));

        // Sıralı olarak line'ları takip et
        for (int i = 0; i < lines.size(); i++) {
            Line currentLine = lines.get(i);
            Point2D endPoint = new Point2D(currentLine.getEndX(), currentLine.getEndY());

            // Son nokta ilk noktayla aynı değilse ekle (polygon kapanışını kontrol et)
            if (i < lines.size() - 1 || !endPoint.equals(orderedPoints.get(0))) {
                orderedPoints.add(endPoint);
            }
        }

        // Nokta sayısı
        int n = orderedPoints.size();
        if (n < 3) {
            // Polygon oluşturmak için en az 3 nokta gerekli
            return new Point2D(0, 0);
        }

        // X ve Y dizilerini oluştur
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = orderedPoints.get(i).getX();
            y[i] = orderedPoints.get(i).getY();
        }

        double a, cx, cy, t;
        int i, i1;

        /* First calculate the polygon's signed area A */
        a = 0.0;
        i1 = 1;
        for (i = 0; i < n; i++) {
            a += x[i] * y[i1] - x[i1] * y[i];
            i1 = (i1 + 1) % n;
        }
        a *= 0.5;

        // Alan sıfırsa (dejenere polygon) merkez hesaplanamaz
        if (Math.abs(a) < 0.0001) {
            // Basit ortalama döndür
            double avgX = 0, avgY = 0;
            for (i = 0; i < n; i++) {
                avgX += x[i];
                avgY += y[i];
            }
            return new Point2D(avgX / n, avgY / n);
        }

        /* Now calculate the centroid coordinates Cx and Cy */
        cx = cy = 0.0;
        i1 = 1;
        for (i = 0; i < n; i++) {
            t = x[i] * y[i1] - x[i1] * y[i];
            cx += (x[i] + x[i1]) * t;
            cy += (y[i] + y[i1]) * t;
            i1 = (i1 + 1) % n;
        }
        cx = cx / (6.0 * a);
        cy = cy / (6.0 * a);

        return new Point2D(cx, cy);
    }


    public Lbl getLabel() {
        return lbl;
    }


    public void setLabel(Lbl lbl) {
        this.lbl = lbl;
        if(lbl != null)
        lbl.changeLabelPosition(center);
    }


    public Point2D getCenter() {
        return center;
    }


    public BoundingBox getBounds() {
        return bounds;
    }


    public boolean isCalculatedCenterPointIsInThisShape() {
        return calculatedCenterPointIsInThisShape;
    }


    public List<Line> getLines() {
        return lines;
    }


    public boolean isAvalidPiece() {
        double area = this.bounds.getWidth() * this.bounds.getHeight();
        boolean result = area > 850 && area < 1_500_000;
        return result;
    }


    public boolean isShapeSelected() {
        return shapeSelected;
    }


    public void setShapeSelected(boolean shapeSelected) {
        this.shapeSelected = shapeSelected;
    }


    public Color getColor() {
        return color;
    }


    public void setColor(Color color) {
        this.color = color;
    }


    public void restoreColor() {
        setColor(originalColor);
    }


    public void reAnalyse() {
        analysePath(lines);
    }


    public boolean isShapePrinted() {
        return shapePrinted;
    }


    public void setShapePrinted(boolean shapePrinted) {
        this.shapePrinted = shapePrinted;
    }


    private void analysePath(List<Line> lines) {
        double xMax = 0;
        double yMax = 0;
        double xMin = Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        for (Line l : lines) {
            double endpx = l.getEndX();
            double endpy = l.getEndY();
            double startpx = l.getStartX();
            double startpy = l.getStartY();
            if (endpx > xMax) {
                xMax = endpx;
            }
            if (endpy > yMax) {
                yMax = endpy;
            }
            if (startpx < xMin) {
                xMin = startpx;
            }
            if (startpy < yMin) {
                yMin = startpy;
            }
        }
        this.minX = xMin;
        this.maxX = xMax;
        this.minY = yMin;
        this.maxY = yMax;
        center = isGGTFile ? calculateCentroidGGT(lines): calculateCentroid(lines);
        calculatedCenterPointIsInThisShape = pointInPolygon(lines, center);
        bounds = new BoundingBox(this.minX, this.minY, (this.maxX - this.minX), (this.maxY - this.minY));
    }


    public void mirrorX(double pastalW) {
        double labelXoffset = pastalW;
        for (Line line : lines) {
            line.setStartX(labelXoffset - line.getStartX());
            line.setEndX(labelXoffset - line.getEndX());
        }
        analysePath(lines);
        relocateTheOriginInXaxis();
    }


    public void mirrorY(double pastalH) {
        double labelYoffset = pastalH;
        for (Line line : lines) {
            line.setStartY(labelYoffset - line.getStartY());
            line.setEndY(labelYoffset - line.getEndY());
        }
        analysePath(lines);
        relocateTheOriginInXaxis();
    }


    /**
     * Parcanin x ekseninde orjini yeniden belirler.
     * <p>
     * Belirlenen orijin zaten parcanin icerisindeyse, once parcadan disari
     * cikar,ciktigi noktayi kayit eder, sonra tekrar parcanin icine girer ve diger
     * taraftaki sinirini tespit eder ve ortalamasini yeni merkez olarak belirler.
     * <p>
     * Belirlenen orijin parcanin disinda kaldiyas once parcanin icine girmeye
     * calisir, icine girdigi noktayi kayit eder. Sonra aksi istikamette tekrar
     * parcadan disari cikmaya calisir ve ciktigi noktayi ikinci nokta olarak kayit
     * eder. Daha sonra iki noktanin ortalamasini yeni merkez olarak belirler.
     */
    public void relocateTheOriginInXaxis() {
        /**
         * True ise ;Bu parca icin tespit edilen ilk merkez zaten parcanin icinde
         * kalmistir, fakat biz parcanin disina cikip tekrar icine girerek sinirlarini
         * belirleyip, yeniden merkezleme yapacagiz.
         *
         * false; ise once parcanin icerisine girer sonra ciktigi yeri buluruz.
         */
        boolean alreadyIn = isCalculatedCenterPointIsInThisShape();
        boolean forward = true; // varsayilan olarak ileri yonde aramaya baslasin.
        boolean backward = false;
        double originalX = center.getX();
        /**
         * 1 ise pozitif yonde arama yaparken istenilen sonuca ulasilmisir. -1 ise
         * negatif yonde.
         */
        int lastDirection = 0;
        /**
         * Merkezi hesaplamak icin ilk tespit edilen nokta.
         */
        double firstIntersectionPoint = 0;
        /**
         * Merkezi hesaplamak icin ikinci tespit edilen nokta.
         */
        double secondIntersectionPoint = 0;
        /**
         * Sinir aramasi yaparken bulunamazsa deneme sayisi sonra islemi
         * sonlandiracagiz.
         */
        int tryCount = 0;
        /**
         * Bir parca icin o parcanin sinirlarini arama alani pastalin en buyuk olan
         * kenari kadar olacaktir. Cunku pastal uzunlugu 2m olan yatay veya dikey
         * parcalardan bile olusabilir.
         */
        int searchDistance = (int) maxX;
        /**
         * Isleme baslandiginda merkez parcanin icerisindeyse, disina cikana kadar;
         * Isleme baslandiginda merkez parcanin disindayse , icine girene kadar tara !
         */
        while (alreadyIn ? pointInPolygon(lines, center) : !pointInPolygon(lines, center)) {
            if (tryCount++ >= searchDistance * 2) { // ileri geri aradi ama orta nokta bulamadi,
                String errMsg = "Error while detection of the center. There may be interwoven parts in the cad file. ->";
                System.err.println(errMsg);
                return;
            }
            if (center == null || center.getX() == Double.POSITIVE_INFINITY || center.getX() == Double.NEGATIVE_INFINITY || center.getX() == Double.NaN)
                return;
            /**
             * Pozitif yonde bulamadik, geriye taramayi baslat.
             */
            if ((center.getX() - originalX) >= searchDistance) {
                // System.out.println("backward");
                forward = false;
                backward = true;
                center = new Point2D(originalX, center.getY()); // restore
            }
            /**
             *
             * Negatif yonde bulamadik, ileriye dogru taramaya basla. Ayni zamanda forward
             * biti initial value true dur.Aramaya once pozitif yonde basliyoruz.
             */
            if ((center.getX() - originalX) <= -searchDistance) {
                // System.out.println("forward");
                forward = true;
                backward = false;
                center = new Point2D(originalX, center.getY());// restore
            }
            if (forward) {
                center = new Point2D(center.getX() + 1, center.getY()); // merkezi pozitif yonde kaydir.
            }
            if (backward) {
                center = new Point2D(center.getX() - 1, center.getY()); // merkezi negatif yonde kaydir.
            }
        }
        /**
         * Tespit edilen son nokta iler yonde mi geri yonde ilerlerken mi bulundu?
         */
        if (forward)
            lastDirection = 1;
        if (backward)
            lastDirection = -1;
        firstIntersectionPoint = center.getX();
        /**
         * Baslarken zaten icinde isek, en basta disari cikmistik. Simdi tekrar icer
         * girelim.
         */
        if (alreadyIn) {
            while (!pointInPolygon(lines, center)) {
                /**
                 * Ileri giderken bulunduysa iceri girmek icin geri git.
                 */
                if (lastDirection == 1) {
                    center = new Point2D(center.getX() - 1, center.getY());
                }
                /**
                 * Geri giderken bulunduysa iceri girmek icin ileri git.
                 */
                if (lastDirection == -1) {
                    center = new Point2D(center.getX() + 1, center.getY());
                }
            }
        }
        /**
         * Burada her durumda parcanin icersindeyiz. Simdi tekrar disari cik.
         */
        while (pointInPolygon(lines, center)) {
            if (lastDirection == 1) {
                if (alreadyIn) // baslangicta icerdeysek ve pozitif yonde ilk kesisim bulunduyda terse don
                    center = new Point2D(center.getX() - 1, center.getY()); // merkezi negatif yonde kaydir.
                else
                    center = new Point2D(center.getX() + 1, center.getY()); // merkezi pozitif yonde kaydir.
            }
            if (lastDirection == -1) {
                if (alreadyIn)
                    center = new Point2D(center.getX() + 1, center.getY()); // merkezi negatif yonde kaydir.
                else
                    center = new Point2D(center.getX() - 1, center.getY()); // merkezi negatif yonde kaydir.
            }
        }
        /**
         * Burada artik iki adet kesisim noktasi tespit edildi. orta noktasini hesapla
         * ve yeni merkez olarak belirle.
         */
        secondIntersectionPoint = center.getX();
        double newCenter = 0;
        if (lastDirection == 1)
            newCenter = firstIntersectionPoint + ((secondIntersectionPoint - firstIntersectionPoint) / 2);
        if (lastDirection == -1)
            newCenter = firstIntersectionPoint - (firstIntersectionPoint - secondIntersectionPoint) / 2;
        center = new Point2D(newCenter, center.getY());
        if (!alreadyIn) {
            calculatedCenterPointIsInThisShape = true;
        }
        relocateTheOriginInYaxis();
    }


    /**
     * * Parcanin y ekseninde orjini yeniden belirler.
     * <p>
     * Belirlenen orijin zaten parcanin icerisindeyse, once parcadan disari
     * cikar,ciktigi noktayi kayit eder, sonra tekrar parcanin icine girer ve diger
     * taraftaki sinirini tespit eder ve ortalamasini yeni merkez olarak belirler.
     * <p>
     * Belirlenen orijin parcanin disinda kaldiyas once parcanin icine girmeye
     * calisir, icine girdigi noktayi kayit eder. Sonra aksi istikamette tekrar
     * parcadan disari cikmaya calisir ve ciktigi noktayi ikinci nokta olarak kayit
     * eder. Daha sonra iki noktanin ortalamasini yeni merkez olarak belirler.
     */
    public void relocateTheOriginInYaxis() {
        /**
         * True ise ;Bu parca icin tespit edilen ilk merkez zaten parcanin icinde
         * kalmistir, fakat biz parcanin disina cikip tekrar icine girerek sinirlarini
         * belirleyip, yeniden merkezleme yapacagiz.
         *
         * false; ise once parcanin icerisine girer sonra ciktigi yeri buluruz.
         */
        boolean alreadyIn = isCalculatedCenterPointIsInThisShape();
        boolean up = true; // varsayilan olarak ileri yonde aramaya baslasin.
        boolean down = false;
        double originalY = center.getY();
        /**
         * 1 ise pozitif yonde arama yaparken istenilen sonuca ulasilmisir. -1 ise
         * negatif yonde.
         */
        int lastDirection = 0;
        /**
         * Merkezi hesaplamak icin ilk tespit edilen nokta.
         */
        double firstIntersectionPoint = 0;
        /**
         * Merkezi hesaplamak icin ikinci tespit edilen nokta.
         */
        double secondIntersectionPoint = 0;
        /**
         * Sinir aramasi yaparken bulunamazsa deneme sayisi sonra islemi
         * sonlandiracagiz.
         */
        int tryCount = 0;
        /**
         * Bir parca icin o parcanin sinirlarini arama alani pastalin en buyuk olan
         * kenari kadar olacaktir. Cunku pastal uzunlugu 2m olan yatay veya dikey
         * parcalardan bile olusabilir.
         */
        int searchDistance = (int) maxY;
        /**
         * Isleme baslandiginda merkez parcanin icerisindeyse, disina cikana kadar;
         * Isleme baslandiginda merkez parcanin disindayse , icine girene kadar tara !
         */
        while (alreadyIn ? pointInPolygon(lines, center) : !pointInPolygon(lines, center)) {
            if (tryCount++ >= searchDistance * 2) { // ileri geri aradi ama orta nokta bulamadi
                // TODO
                //MainController.showAlert("Warning!", new String[] { "Error while detection of the center. There may be interwoven parts in the cad file." }, AlertType.WARNING, null);
                return;
            }
            if (center == null || center.getY() == Double.POSITIVE_INFINITY || center.getY() == Double.NEGATIVE_INFINITY || center.getY() == Double.NaN)
                return;
            /**
             * Pozitif yonde bulamadik, geriye taramayi baslat.
             */
            if ((center.getY() - originalY) >= searchDistance) {
                // System.out.println("backward");
                up = false;
                down = true;
                center = new Point2D(center.getX(), originalY); // restore
            }
            /**
             *
             * Negatif yonde bulamadik, ileriye dogru taramaya basla. Ayni zamanda forward
             * biti initial value true dur.Aramaya once pozitif yonde basliyoruz.
             */
            if ((center.getY() - originalY) <= -searchDistance) {
                // System.out.println("forward");
                up = true;
                down = false;
                center = new Point2D(center.getX(), originalY);// restore
            }
            if (up) {
                center = new Point2D(center.getX(), center.getY() + 1); // merkezi pozitif yonde kaydir.
            }
            if (down) {
                center = new Point2D(center.getX(), center.getY() - 1); // merkezi negatif yonde kaydir.
            }
        }
        /**
         * Tespit edilen son nokta iler yonde mi geri yonde ilerlerken mi bulundu?
         */
        if (up)
            lastDirection = 1;
        if (down)
            lastDirection = -1;
        firstIntersectionPoint = center.getY();
        /**
         * Baslarken zaten icinde isek, en basta disari cikmistik. Simdi tekrar icer
         * girelim.
         */
        if (alreadyIn) {
            while (!pointInPolygon(lines, center)) {
                /**
                 * Ileri giderken bulunduysa iceri girmek icin geri git.
                 */
                if (lastDirection == 1) {
                    center = new Point2D(center.getX(), center.getY() - 1);
                }
                /**
                 * Geri giderken bulunduysa iceri girmek icin ileri git.
                 */
                if (lastDirection == -1) {
                    center = new Point2D(center.getX(), center.getY() + 1);
                }
            }
        }
        /**
         * Burada her durumda parcanin icersindeyiz. Simdi tekrar disari cik.
         */
        while (pointInPolygon(lines, center)) {
            if (lastDirection == 1) {
                if (alreadyIn) // baslangicta icerdeysek ve pozitif yonde ilk kesisim bulunduyda terse don
                    center = new Point2D(center.getX(), center.getY() - 1); // merkezi negatif yonde kaydir.
                else
                    center = new Point2D(center.getX(), center.getY() + 1); // merkezi pozitif yonde kaydir.
            }
            if (lastDirection == -1) {
                if (alreadyIn)
                    center = new Point2D(center.getX(), center.getY() + 1); // merkezi negatif yonde kaydir.
                else
                    center = new Point2D(center.getX(), center.getY() - 1); // merkezi negatif yonde kaydir.
            }
        }
        /**
         * Burada artik iki adet kesisim noktasi tespit edildi. orta noktasini hesapla
         * ve yeni merkez olarak belirle.
         */
        secondIntersectionPoint = center.getY();
        double newCenter = 0;
        if (lastDirection == 1)
            newCenter = firstIntersectionPoint + ((secondIntersectionPoint - firstIntersectionPoint) / 2);
        if (lastDirection == -1)
            newCenter = firstIntersectionPoint - (firstIntersectionPoint - secondIntersectionPoint) / 2;
        center = new Point2D(center.getX(), newCenter);
    }


    @Override
    public String toString() {
        return "ClosedShape [lbl=" + lbl + ", center=" + center + "]";
    }


    public void printLinePoints(List<Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines) {
            sb.append(line.getEndX()).append(" , ").append(line.getEndY()).append(" , ")
                    .append(line.getStartX()).append(" , ").append(line.getStartY()).append(" , ");
        }
        // Remove the last extra comma and space
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 3);
        }
        System.out.println(sb.toString());
    }


    public Lbl getLbl() {
        return lbl;
    }


    public void setID(Integer key) {
    this.ID = key;
    }


    public Integer getID() {
        return ID;
    }
}
