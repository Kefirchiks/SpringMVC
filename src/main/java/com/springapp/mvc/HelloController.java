package com.springapp.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

	/*
     Nodokļu normatīvajos aktos ir noteikta kārtība, kādā piemēro nodokļu atvieglojumus, proti, pirms
     darbinieka ienākuma aplikšanas ar algas nodokli no mēneša ienākuma tiek atskaitīti:
     1. Valsts sociālās apdrošināšanas maksājumi (2014.gadā darba ņēmēja obligāto iemaksu likme 10,50%);
     2. Mēneša neapliekamais minimums, kura apmēru taksācijas gadam nosaka Ministru kabinets (2014.gadā – 75 eiro mēnesī);
     3. Paredzētie atvieglojumi Ministru kabineta noteiktajā apmērā
     (2014.gadā iedzīvotāju ienākuma nodokļa atvieglojuma apmērs par apgādībā esošu personu ir 165 eiro mēnesī).
     */

@Controller
@RequestMapping("/")
public class HelloController {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(ModelMap map) {
        return "index";
    }

    private BigDecimal atvieglojumsCount = null;
    private BigDecimal atvieglojumsBruto = null;
    private BigDecimal neto = null;

    HashMap<String, String> currencies = null; // Saraksts ar valūtām + vērtībām

    @RequestMapping("/calculate")
    public String calculate(HttpServletRequest request, ModelMap map) {
        try {
            if (request.getParameter("bruto") != null && !"".equals(request.getParameter("bruto"))) {

                // Parasti es rakstītu DTO objektu, kuru elementus inicializācijas laikā nonullētu.
                setAtvieglojumsBruto(null);
                setAtvieglojumsCount(null);

                getAtvieglojumsMethod(request, map);
                BigDecimal bruto = new BigDecimal(request.getParameter("bruto"));

                // Uzņēmēja nodokļi:
                BigDecimal uznSoc = bruto.multiply(getUznSocIem()).divide(getHundred(), MathContext.DECIMAL32);
                uznSoc = uznSoc.setScale(2, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal uznKopIzm = uznSoc.add(getUznRiskNodeva()).add(bruto);
                uznKopIzm = uznKopIzm.setScale(2, BigDecimal.ROUND_HALF_EVEN);

                map.put("socIem", uznSoc + "€");
                map.put("risks", getUznRiskNodeva() + "€");
                map.put("kopa", uznKopIzm + "€");

                BigDecimal social = bruto.multiply(getSocialTax()).divide(getHundred(), MathContext.DECIMAL32);

                // Pielietojam atvieglojumus
                if (getAtvieglojumsCount() != null) {
                    BigDecimal atvieglojums = getAtvieglojumsCount().multiply(getAtvieglojumsValue());
                    setAtvieglojumsBruto(bruto.subtract(atvieglojums));
                    // IIN aprēķinam no bruto summas, kurai piemērots personu apgādības atvieglojums
                    BigDecimal IIN = (getAtvieglojumsBruto().subtract(social).subtract(getNeapliekMin())).multiply(getIINTax()).divide(getHundred(), MathContext.DECIMAL32);
                    // Pieskaitam klāt atvieglojumus
                    setNeto(getAtvieglojumsBruto().subtract(social).subtract(IIN).add(atvieglojums));
                } else {
                    // Rēķinam no bruto, kuram nav pievienots atvieglojums
                    BigDecimal IIN = (bruto.subtract(social).subtract(getNeapliekMin())).multiply(getIINTax()).divide(getHundred(), MathContext.DECIMAL32);
                    setNeto(bruto.subtract(social).subtract(IIN));
                }
                setNeto(getNeto().setScale(2, BigDecimal.ROUND_HALF_EVEN));
                map.put("neto", getNeto() + "€");


                // Atlasam valūtas un exchange rate no XMLa
                parseXML();
                map.put("currencies", currencies);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "index";
    }

    public void getAtvieglojumsMethod(HttpServletRequest request, ModelMap map) {
        try {
            if (request.getParameter("atvieglojumi") != null && !"".equals(request.getParameter("atvieglojumi"))) {
                setAtvieglojumsCount(new BigDecimal(request.getParameter("atvieglojumi")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void parseXML() throws IOException, SAXException, ParserConfigurationException {

        String uri = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
        URL url = new URL(uri);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(url.openStream());

        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("Cube");
        currencies = new HashMap<String, String>();
        for (int elem = 0; elem < nList.getLength(); elem++) {

            Node nNode = nList.item(elem);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                Element eElement = (Element) nNode;
                String currency = eElement.getAttribute("currency");
                String rate = eElement.getAttribute("rate");
                if (currency.isEmpty()) {
                    continue;
                }
                currencies.put(currency, rate);
            }
        }
    }


    public static BigDecimal getSocialTax() {
        return BigDecimal.valueOf(10.5);
    }

    public static BigDecimal getHundred() {
        return BigDecimal.valueOf(100);
    }

    public static BigDecimal getIINTax() {
        return BigDecimal.valueOf(23);
    }

    public static BigDecimal getNeapliekMin() {
        return BigDecimal.valueOf(75);
    }

    public static BigDecimal getAtvieglojumsValue() {
        return BigDecimal.valueOf(165);
    }

    public static BigDecimal getUznSocIem() {
        return BigDecimal.valueOf(23.59);
    }

    public static BigDecimal getUznRiskNodeva() {
        return BigDecimal.valueOf(0.36);
    }

    public BigDecimal getAtvieglojumsCount() {
        return atvieglojumsCount;
    }

    public void setAtvieglojumsCount(BigDecimal atvieglojumsCount) {
        this.atvieglojumsCount = atvieglojumsCount;
    }

    public BigDecimal getNeto() {
        return neto;
    }

    public void setNeto(BigDecimal neto) {
        this.neto = neto;
    }

    public BigDecimal getAtvieglojumsBruto() {
        return atvieglojumsBruto;
    }

    public void setAtvieglojumsBruto(BigDecimal atvieglojumsBruto) {
        this.atvieglojumsBruto = atvieglojumsBruto;
    }
}