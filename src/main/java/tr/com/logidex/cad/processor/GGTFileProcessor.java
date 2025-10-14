package tr.com.logidex.cad.processor;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.model.Label;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GGTFileProcessor extends FileProcessor {

    private List<GGTPattern> parcalar;
    private GGTPattern aktifGGTPattern;
    private final String fileContent;

    public GGTFileProcessor(String fileContent) {
        super(fileContent);
        this.parcalar = new ArrayList<>();
        this.aktifGGTPattern = null;
        this.fileContent = fileContent;
    }

    @Override
    protected void initUnwantedChars() {
        UNWANTED_CHARS = Arrays.asList("", null, "\32", "\n", "\r");
    }

    @Override
    protected void initSplitRegex() {
        SPLIT_REGEX = "N\\d+\\*";
    }

    @Override
    protected void interpretCommands() {
        parcalar = parse(fileContent);

        for (GGTPattern GGTPattern : parcalar) {
            System.out.println("=== Parça: " + GGTPattern.getId() + " ===");

            List<Line> parcaLines = GGTPattern.getLines();
            List<Line> scaledLines = new ArrayList<>();

            for (Line line : parcaLines) {
                Line scaledLine = new Line(
                        this.scale(line.getStartX()), this.scale(line.getStartY()),
                        this.scale(line.getEndX()), this.scale(line.getEndY())
                );
                scaledLines.add(scaledLine);
            }

            super.lines.addAll(scaledLines);
            super.linesForClosedShapes.put(GGTPattern.getId(),scaledLines);
            super.getGGTPatterns().add(GGTPattern);



            System.out.println("Line sayısı: " + scaledLines.size());
            System.out.println("------------------------");
            System.out.println(GGTPattern.getId() + " ->" + GGTPattern.getLabel());
            //System.out.println(parca.getLines().get(parca.getLines().size() -1 ));
        }
    }

    public List<GGTPattern> parse(String dosyaIcerigi) {
        parcalar.clear();

        // Parça başlangıçlarını bul
        Pattern parcaBaslangiciPattern = Pattern.compile("N(\\d+)\\*");
        Matcher parcaBaslangiciMatcher = parcaBaslangiciPattern.matcher(dosyaIcerigi);

        List<Integer> parcaBaslangicIndeksleri = new ArrayList<>();
        List<String> parcaNolari = new ArrayList<>();

        while (parcaBaslangiciMatcher.find()) {
            parcaBaslangicIndeksleri.add(parcaBaslangiciMatcher.start());
            parcaNolari.add(parcaBaslangiciMatcher.group(1));
        }

        // Her parçayı işle
        for (int i = 0; i < parcaBaslangicIndeksleri.size(); i++) {
            int baslangic = parcaBaslangicIndeksleri.get(i);
            int bitis = (i < parcaBaslangicIndeksleri.size() - 1) ?
                    parcaBaslangicIndeksleri.get(i + 1) : dosyaIcerigi.length();

            Integer parcaNo = Integer.parseInt(parcaNolari.get(i));
            String parcaIcerigi = dosyaIcerigi.substring(baslangic, bitis);
            System.out.println(parcaIcerigi + "\n");

            aktifGGTPattern = new GGTPattern(parcaNo);
            parcalar.add(aktifGGTPattern);


            // Etiket bilgisini parse et
            parseEtiket(parcaIcerigi,0);


            parseKomutlar(removeM15BetweenM19AndM31(fixM19Discontinuities(parcaIcerigi)));


        }

        return parcalar;
    }

    public static String fixM19Discontinuities(String input) {
        // Pattern: M19*X[sayı]Y[sayı]*M15*X[sayı]Y[sayı]*M14
        // Sadece M19, M15 ve M14'ü sil, koordinatları koru
        String pattern = "\\*M19(\\*X\\d+Y\\d+)\\*M15\\*X\\d+Y\\d+\\*M14";

        // $1 ile sadece M19'dan sonraki koordinatı koru
        return input.replaceAll(pattern, "$1");
    }

    public static String removeM15BetweenM19AndM31(String input) {
        // M19 + koordinat + M15 + M31 pattern'ini bul
        // M15'i sil, gerisini koru
        String pattern = "(\\*M19\\*X\\d+Y\\d+)\\*M15(\\*M31)";

        // $1 = *M19*X6346Y4285
        // $2 = *M31
        return input.replaceAll(pattern, "$1$2");
    }


    private void parseEtiket(String parcaIcerigi,int searchIndex) {


        // M31 ile başlayan etiket pattern'i: M31*X<sayı>Y<sayı>*<metin>*


        Pattern etiketPattern = Pattern.compile("M31\\*X(-?\\d+)Y(-?\\d+)\\*([^*]+)\\*");
        Matcher etiketMatcher = etiketPattern.matcher(parcaIcerigi);

        if (etiketMatcher.find(searchIndex)) {
            try {
                double x = Double.parseDouble(etiketMatcher.group(1));
                double y = Double.parseDouble(etiketMatcher.group(2));
                String etiketMetni = etiketMatcher.group(3);




               aktifGGTPattern.getParcaninEtiketleri().put(etiketMetni,new Point2D(x,y));


                int end = etiketMatcher.end();
                parseEtiket(parcaIcerigi,end); // Check whether there is  another M31 command in the pattern.


            } catch (NumberFormatException e) {
                System.err.println("Etiket parse hatası: " + e.getMessage());
            }
        }


        assignALabel();


    }

    private void assignALabel() {

        var textAndPositions = aktifGGTPattern.getParcaninEtiketleri();


        if(! textAndPositions.isEmpty()){

            StringBuilder labelTextBuilder = new StringBuilder();

            AtomicReference<Point2D> pos = new AtomicReference<>(new Point2D(0, 0));
            textAndPositions.forEach((k,v) -> {
                labelTextBuilder.append(k+"\n");
                pos.set(v);

            });

            Label createdLabel = new Label(labelTextBuilder.toString(), pos.get(),0,2,12,12);


            aktifGGTPattern.setEtiket(createdLabel);



        }
    }



    private void parseKomutlar(String parcaIcerigi) {
        String[] komutlar = parcaIcerigi.split("\\*");

        Point2D mevcutPozisyon = null;
        boolean kesmeDurum = false;

        // Her M14-M15 bloğu için ayrı çizim yap
        List<Point2D> kesmeNoktaları = new ArrayList<>();

        for (String komut : komutlar) {
            if (komut == null || komut.trim().isEmpty()) continue;
            komut = komut.trim();

            if (komut.equals("M14")) {
                // Kesme başlıyor
                kesmeDurum = true;
                kesmeNoktaları.clear();

            } else if (komut.equals("M15")) {
                // Kesme bitiyor, çizgileri oluştur
                if (kesmeDurum && kesmeNoktaları.size() > 1) {
                    // Sırayla çizgileri oluştur
                    for (int j = 0; j < kesmeNoktaları.size() - 1; j++) {
                        Point2D baslangic = kesmeNoktaları.get(j);
                        Point2D bitis = kesmeNoktaları.get(j + 1);
                        aktifGGTPattern.cizgiEkle(baslangic, bitis);
                    }

                    // Şekli kapat
                    Point2D ilkNokta = kesmeNoktaları.get(0);
                    Point2D sonNokta = kesmeNoktaları.get(kesmeNoktaları.size() - 1);

                    // Eğer şekil kapalı değilse kapat
                    if (!ilkNokta.equals(sonNokta)) {
                        aktifGGTPattern.cizgiEkle(sonNokta, ilkNokta);
                    }
                }

                kesmeDurum = false;
                kesmeNoktaları.clear();

            } else if (komut.startsWith("X") && komut.contains("Y")) {
                // Koordinat
                Point2D yeniPozisyon = parseKoordinat(komut);

                if (yeniPozisyon != null) {
                    if (kesmeDurum) {
                        kesmeNoktaları.add(yeniPozisyon);
                    }
                    mevcutPozisyon = yeniPozisyon;
                }
            }
            // Diğer komutları atla (M31, D2, renk bilgileri vs.)
        }
    }

    private Point2D parseKoordinat(String komut) {
        Pattern koordinatPattern = Pattern.compile("X(-?\\d+)Y(-?\\d+)");
        Matcher koordinatMatcher = koordinatPattern.matcher(komut);

        if (koordinatMatcher.find()) {
            try {
                double x = Double.parseDouble(koordinatMatcher.group(1));
                double y = Double.parseDouble(koordinatMatcher.group(2));
                return new Point2D(x, y);
            } catch (NumberFormatException e) {
                System.err.println("Koordinat parse hatası: " + komut);
                return null;
            }
        }
        return null;
    }

    public List<GGTPattern> getParcalar() {
        return parcalar;
    }

    public GGTPattern getParcaById(String id) {
        for (GGTPattern GGTPattern : parcalar) {
            if (GGTPattern.getId().equals(id)) {
                return GGTPattern;
            }
        }
        return null;
    }

    @Override
    protected double scale(double number) {
        double value = number * 0.025 * 1.016;
        return value * 10f;
    }
}

class GGTPattern {
    private Integer id;
    private List<Line> lines;
    private Label etiket;
    private final Map<String,Point2D> parcaninEtiketleri = new LinkedHashMap<>();

    public GGTPattern(Integer id) {
        this.id = id;
        this.lines = new ArrayList<>();
        this.etiket = null;
    }

    public Integer getId() {
        return id;
    }

    public void cizgiEkle(Point2D baslangic, Point2D bitis) {
        if (!baslangic.equals(bitis)) {
            Line line = new Line(
                    baslangic.getX(), baslangic.getY(),
                    bitis.getX(), bitis.getY()
            );
            lines.add(line);
        }
    }

    public List<Line> getLines() {
        return lines;
    }

    public Label getLabel() {
        return etiket;

    }

    public void setEtiket(Label label) {
        this.etiket = label;
    }


    public Map<String,Point2D> getParcaninEtiketleri() {
        return parcaninEtiketleri;
    }
}


