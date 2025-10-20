package tr.com.logidex.cad;
import javafx.geometry.Point2D;
import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class LabelGroupingManager {
    private ArrayList<Lbl> lbls;
    private ArrayList<GroupedLabel> listLbg;
    private ArrayList<Lbl> finalLbls;
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
     * @param flipHorizontal                - Pastal x ekseninde aynalanmis mi ?
     * @return ArrayList<Lbl> - Gruplandirilmis ve siraya sokulmus etiketlerin
     * listesidir.
     */
    public List<Lbl> groupAndSortLabels(List<Lbl> gruplandirilacakEtiketListesi, double pastalEnKucukXNoktasi, double toplamPastalUzunlugu, double toplamPastalGenisligi, FlipHorizontally flipHorizontal, FlipVertically flipVertical) {
        lbls = new ArrayList<>(gruplandirilacakEtiketListesi);
        minX = pastalEnKucukXNoktasi;
        maxX = toplamPastalUzunlugu;
        totalW = toplamPastalUzunlugu;
        totalH = toplamPastalGenisligi;
        isFlipHorizontal = flipHorizontal == FlipHorizontally.YES;
        isFlipVertical = flipVertical == FlipVertically.YES;
        listLbg = new ArrayList<GroupedLabel>();
        GroupedLabel lbg = new GroupedLabel();
        Lbl selectedLbl = null;
        Lbl selectedNextLbl = null;
        for (int i = 0; i < lbls.size(); i++) {
            selectedLbl = lbls.get(i);
            try {
                selectedNextLbl = lbls.get(i + 1);
            } catch (Exception e) {
                // TODO: handle exception
            }
            selectedNextLbl = selectedNextLbl == null ? selectedLbl : selectedNextLbl;
            lbg = parse(lbg, selectedLbl, selectedNextLbl);
        }
        // Dongu bittikten sonra son kalan grubu da torbaya atalim.
        listLbg.add(lbg);
        createFinalLabelList(listLbg);
        return finalLbls;
    }


    private GroupedLabel parse(GroupedLabel lbg, Lbl selectedLbl, Lbl selectedNextLbl) {

        if(selectedLbl == null || selectedNextLbl == null)
            return new GroupedLabel();



        // Birbirine paralel iki text(dogru) arasindaki uzakligi bulalim.
        /*
         * Formul : Sqrt(((x1-x2)^2)+((y1-y2)^2))
         *
         *
         */
        double dx = selectedLbl.getPosition().getX() - selectedNextLbl.getPosition().getX();
        double dy = selectedLbl.getPosition().getY() - selectedNextLbl.getPosition().getY();
        double d = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        /*
         * Aradaki mesafe <=20 bu yazilar alt alta gelmislerdir, yani bir etiket grubu
         * uyesi olacaklardir.
         */
        if (d <= 20) {
            // Bu etiketi etiket grubuna ekle
            lbg.add(selectedLbl);
        } else {
            /*
             * Ard arda gelenler bitti demektir.Grup etiketini olusturalim.
             */
            // Sonuncuyu da olusturulan gruba ekle.
            lbg.add(selectedLbl);
            listLbg.add(lbg);
            lbg = new GroupedLabel();
        }
        return lbg;
    }


    private String labelGroup2String(GroupedLabel group) {
        StringBuilder sb = new StringBuilder();
        for (Lbl lb : group) {
            sb.append(lb.getText() + (char) 0x0d); // add carriage return.
        }
        return sb.toString();
    }


    /**
     * Disari aktarilmaya hazir etiket listesini olusturur.
     */
    private void createFinalLabelList(ArrayList<GroupedLabel> groupList) {

        if(groupList == null || groupList.size() == 0)
            return;

        finalLbls = new ArrayList<Lbl>();
        double x = 0;
        double y = 0;
        double pastalW = totalW;
        double pastalH = totalH;
        removeTheLineThatIncludesTheSameText(groupList);
        for (GroupedLabel group : groupList) {

            if(group == null || group .isEmpty())
                continue;
            int refSatir = referansSatiriBelirle(group.size());
            double labelXoffset = isFlipHorizontal ? pastalW : group.get(refSatir).getPosition().getX() * 2;
            double labelYoffset = isFlipVertical ? pastalH : group.get(refSatir).getPosition().getY() * 2;
            // Bir etiketin pozisyonu 'refSatir' elemaninin pozisyonu olsun.
            x = labelXoffset - group.get(refSatir).getPosition().getX();
            y = labelYoffset - group.get(refSatir).getPosition().getY();
            finalLbls.add(new Lbl(labelGroup2String(group), new Point2D(x, y), group.get(refSatir).getAngle(), group.get(refSatir).getOrigin(), group.get(refSatir).getWidth(), group.get(refSatir).getHeight()));
            listedekiIstenmeyenEtiketleriSil();
        }
    }


    private void removeTheLineThatIncludesTheSameText(ArrayList<GroupedLabel> groupList) {
        Lbl tmp = null;
        //Yukaridan asagi dogru tarama yapar.Herhangi bir satirdaki verinin bir parcasi,
        //kendinden once gelen herhangi bir satirdaki yazinin tamamini iceriyorsa , tamami benzeyen satiri sil.
        for (GroupedLabel group : groupList) {
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
        Iterator<Lbl> iter = finalLbls.iterator();
        /*
         * Bu dongunun sonunda elde edilen listenin icerisindeki textler , bir etiketin
         * (LabelGroup) elemani olmaya hak hazanmislardir.
         *
         */
        while (iter.hasNext()) {
            Lbl lbl = iter.next();
            int rowCountOfLabel = getLabelRows(lbl).length;
            double labelPosX = lbl.getPosition().getX();
            /*
             * Plc ye gonderilecek etiket pastalin sinirlari icerisinde olmalidir.Disinda
             * kalan yazilari yeni listemizden siliyoruz. Ayrica erak mavi icin ozel olarak
             * iki satirdan daha fazla etiket bilgisi gelemez.
             */
            if ((labelPosX <= minX || labelPosX >= maxX) || (rowCountOfLabel == 1 && lbl.getText().length() > 100)) {
                // Gecersiz text sil.
                // System.out.println("Silindi : "+lbl.getText());
                iter.remove();
            }
        }
    }


    private String[] getLabelRows(Lbl lbl) {
        String[] rows = lbl.getText().split("\\n");
        return rows;
    }


    public void clear() {
        lbls = null;
        listLbg = null;
        finalLbls = null;
    }
}