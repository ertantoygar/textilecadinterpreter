package tr.com.logidex.cad.processor;
import javafx.geometry.Point2D;
import tr.com.logidex.cad.model.Label;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class LabelGroupingManager {
    private ArrayList<Label> labels;
    private ArrayList<ArrayList<Label>> listLbg;
    private ArrayList<Label> finalLabels;
    private boolean isFlipHorizontal, isFlipVertical;
    private double maxX, minX, totalW, totalH;
    /**
     * This class uses for , group the nearest lbls and create a new label.And
     * makes list with them.
     */
    /**
     * @param gruplandirilacakEtiketListesi - Dosyadan okunan ham etiket listesi
     * @param pastalEnKucukXNoktasi         - Pastalin x eksenindeki ilk
     * @param toplamPastalUzunlugu          - Toplam pastal uzunlugu
     * @param                 - Pastal x ekseninde aynalanmis mi ?
     * @return ArrayList<Lbl> - Gruplandirilmis ve siraya sokulmus etiketlerin
     * listesidir.
     */
    public List<Label> groupAndSortLabels(List<Label> gruplandirilacakEtiketListesi, double pastalEnKucukXNoktasi, double toplamPastalUzunlugu, double toplamPastalGenisligi, FlipDirection flipDirection) {
        labels = new ArrayList<>(gruplandirilacakEtiketListesi);
        minX = pastalEnKucukXNoktasi;
        maxX = toplamPastalUzunlugu;
        totalW = toplamPastalUzunlugu;
        totalH = toplamPastalGenisligi;
        isFlipHorizontal = flipDirection == FlipDirection.HORIZONTAL;
        isFlipVertical = flipDirection == FlipDirection.VERTICAL;
        listLbg = new ArrayList<ArrayList<Label>>();
        ArrayList<Label> lbg = new ArrayList<Label>();
        Label selectedLabel = null;
        Label selectedNextLabel = null;
        for (int i = 0; i < labels.size(); i++) {
            selectedLabel = labels.get(i);
            try {
                selectedNextLabel = labels.get(i + 1);
            } catch (Exception e) {
                // TODO: handle exception
            }
            selectedNextLabel = selectedNextLabel == null ? selectedLabel : selectedNextLabel;
            lbg = parse(lbg, selectedLabel, selectedNextLabel);
        }
        // Dongu bittikten sonra son kalan grubu da torbaya atalim.
        listLbg.add(lbg);
        createFinalLabelList(listLbg);
        return finalLabels;
    }


    private ArrayList<Label> parse(ArrayList<Label> lbg, Label selectedLabel, Label selectedNextLabel) {

        if(selectedLabel == null || selectedNextLabel == null)
            return new ArrayList<Label>();



        // Birbirine paralel iki text(dogru) arasindaki uzakligi bulalim.
        /*
         * Formul : Sqrt(((x1-x2)^2)+((y1-y2)^2))
         *
         *
         */
        double dx = selectedLabel.getPosition().getX() - selectedNextLabel.getPosition().getX();
        double dy = selectedLabel.getPosition().getY() - selectedNextLabel.getPosition().getY();
        double d = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        /*
         * Aradaki mesafe <=20 bu yazilar alt alta gelmislerdir, yani bir etiket grubu
         * uyesi olacaklardir.
         */
        if (d <= 20) {
            // Bu etiketi etiket grubuna ekle
            lbg.add(selectedLabel);
        } else {
            /*
             * Ard arda gelenler bitti demektir.Grup etiketini olusturalim.
             */
            // Sonuncuyu da olusturulan gruba ekle.
            lbg.add(selectedLabel);
            listLbg.add(lbg);
            lbg = new ArrayList<Label>();
        }
        return lbg;
    }


    private String labelGroup2String(ArrayList<Label> group) {
        StringBuilder sb = new StringBuilder();
        for (Label lb : group) {
            sb.append(lb.getText() + (char) 0x0d); // add carriage return.
        }
        return sb.toString();
    }


    /**
     * Disari aktarilmaya hazir etiket listesini olusturur.
     */
    private void createFinalLabelList(ArrayList<ArrayList<Label>> groupList) {

        if(groupList == null || groupList.size() == 0)
            return;

        finalLabels = new ArrayList<Label>();
        double x = 0;
        double y = 0;
        double pastalW = totalW;
        double pastalH = totalH;
        removeTheLineThatIncludesTheSameText(groupList);
        for (ArrayList<Label> group : groupList) {

            if(group == null || group .isEmpty())
                continue;
            int refSatir = referansSatiriBelirle(group.size());
            double labelXoffset = isFlipHorizontal ? pastalW : group.get(refSatir).getPosition().getX() * 2;
            double labelYoffset = isFlipVertical ? pastalH : group.get(refSatir).getPosition().getY() * 2;
            // Bir etiketin pozisyonu 'refSatir' elemaninin pozisyonu olsun.
            x = labelXoffset - group.get(refSatir).getPosition().getX();
            y = labelYoffset - group.get(refSatir).getPosition().getY();
            finalLabels.add(new Label(labelGroup2String(group), new Point2D(x, y), group.get(refSatir).getAngle(), group.get(refSatir).getOrigin(), group.get(refSatir).getWidth(), group.get(refSatir).getHeight()));
            listedekiIstenmeyenEtiketleriSil();
        }
    }


    private void removeTheLineThatIncludesTheSameText(ArrayList<ArrayList<Label>> groupList) {
        Label tmp = null;
        //Yukaridan asagi dogru tarama yapar.Herhangi bir satirdaki verinin bir parcasi,
        //kendinden once gelen herhangi bir satirdaki yazinin tamamini iceriyorsa , tamami benzeyen satiri sil.
        for (ArrayList<Label> group : groupList) {
            for (int i = 0; i < group.size(); i++) {
                tmp = group.get(i);
                for (int j = i + 1; j < group.size(); j++) {
                    if (group.get(j).getText().contains(tmp.getText())) {
                        group.remove(tmp);
                    }
                }
            }
        }
    }


    /*
     * Etiketin satir sayisina gore pozisyonu verilecek etiket degisecek.Ornegin, 4
     * satirli bir etiketin 2. satirinin pozisyonu etiketin pozisyonu olarak
     * belirlecek. Amacimiz etiketin ortasina yakin bir koordinat belirlemek.
     */
    private int referansSatiriBelirle(int rowCount) {
        int val = 0;
        switch (rowCount) {
            case 0:
                val = 0;
                break;
            case 1:
                val = 0;
                break;
            case 2:
                val = 1;
                break;
            case 3:
                val = 2;
                break;
            case 4:
                val = 2;
                break;
            case 5:
                val = 2;
                break;
            case 6:
                val = 2;
                break;
            case 7:
                val = 3;
                break;
            default:
                break;
        }
        return val;
    }


    private void listedekiIstenmeyenEtiketleriSil() {
        Iterator<Label> iter = finalLabels.iterator();
        /*
         * Bu dongunun sonunda elde edilen listenin icerisindeki textler , bir etiketin
         * (LabelGroup) elemani olmaya hak hazanmislardir.
         *
         */
        while (iter.hasNext()) {
            Label label = iter.next();
            int rowCountOfLabel = getLabelRows(label).length;
            double labelPosX = label.getPosition().getX();
            /*
             * Plc ye gonderilecek etiket pastalin sinirlari icerisinde olmalidir.Disinda
             * kalan yazilari yeni listemizden siliyoruz. Ayrica erak mavi icin ozel olarak
             * iki satirdan daha fazla etiket bilgisi gelemez.
             */
            if ((labelPosX <= minX || labelPosX >= maxX) || (rowCountOfLabel == 1 && label.getText().length() > 100)) {
                // Gecersiz text sil.
                // System.out.println("Silindi : "+lbl.getText());
                iter.remove();
            }
        }
    }


    private String[] getLabelRows(Label label) {
        String[] rows = label.getText().split("\\n");
        return rows;
    }


    public void clear() {
        labels = null;
        listLbg = null;
        finalLabels = null;
    }
}