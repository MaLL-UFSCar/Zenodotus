/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package WebParser;

import NeLL.KB;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author WesleyW
 */
public class Zenodotus {

    public static final String KNOWLEDGE_BROWSER_URL = "http://rtw.ml.cmu.edu/rtw/kbbrowser/";

    /**
     * Lista "todas" as categorias da base do NELL em um JSONArray
     *
     * @return
     */
    public JSONArray getCategoriesList() {
        JSONArray result = new JSONArray();
        Document doc = null;
        try {

            boolean connected = false;
            boolean conected = false;
            while (!conected) {
                try {

                    //Carregando a página
                    doc = Jsoup.connect(KNOWLEDGE_BROWSER_URL + "ontology.php?mode=cat").get();
                    conected = true;

                } catch (Exception e) {

                    System.out.println("[" + Thread.currentThread().getId() + "] No connection to get categories!");
                    e.printStackTrace();
                    Thread.sleep(3000);
                }

            }
            //Primeira seleção de elementos pela tag "li"    
            Elements elements = doc.getElementsByTag("li");

            //Percorrendo todos os elementos encontrados
            for (int ie = 0; ie < elements.size(); ie++) {

                //Selecionando apenas os elementos com a classe "name" (apenas as categorias)
                Elements eName = elements.get(ie).getElementsByAttribute("name");
                if (eName.size() > 0) {
                    //Armazenando o nome e link de cada categoria em um JSONObject
                    Elements href = elements.get(ie).getElementsByAttribute("href");

                    JSONObject jCategory = new JSONObject();
                    jCategory.put(KB.CATEGORY_NAME, eName.first().attr("name"));
                    String link = "http://rtw.ml.cmu.edu/rtw/kbbrowser/list.php?pred=" + jCategory.get(KB.CATEGORY_NAME).toString();
                    jCategory.put(KB.LINK, link);

                    //Adicionando a categoria ao resultado
                    result.add(jCategory);

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Retorna dados de uma categoria em um JSONObject
     *
     * @param pred
     * @return
     */
    public JSONObject getCategory(String pred) {
        JSONObject jCategory = new JSONObject();

        //Verificar se o nome da categoria existe
        JSONArray jaCategories = getCategoriesList();

        int ic = 0;
        boolean match = false;

        while (!match && ic < jaCategories.size()) {
            jCategory = (JSONObject) jaCategories.get(ic);

            if (pred.equals(jCategory.get(KB.CATEGORY_NAME))) {
                match = true;
            }

            ic++;
        }

        if (match) {

            //Carregar página da categoria
            Document doc = null;
            try {
                //TODO: Passar url por arquivo de setup

                boolean conected = false;
            while(!conected){
            try{
                
            doc = Jsoup.connect(jCategory.get(KB.LINK).toString()).timeout(30000).get();
            conected = true;
            
            }catch(Exception e){
                
                System.out.println("[ZENODOTUS] No connection! trying again in 3 seconds...");
                e.printStackTrace();
                Thread.sleep(3000);
            }
            
            }
                
                
                

                //System.out.println(doc);
                //Coletar dados e inserir em um JSONObject
                //Prologo
                Elements ePrologue = doc.getElementsByAttributeValue("class", "prologue_r");
                if (ePrologue.size() > 0) {
                    jCategory.put(KB.CATEGORY_PROLOGUE, ePrologue.first().text());
                }
                //Instâncias
                JSONArray jaInstances = new JSONArray();
                //TODO: Não está carregando toda a página, RESOLVER ISSO!!!
                //TODO: pode existir mais de uma página com resultados, fazer percorrer todas
                do {
                    Elements eInstances = doc.getElementsByAttributeValue("class", "instance");
                    //System.out.println(eInstances);

                    Elements eNames = doc.select("table td:eq(0)");
                    Elements eIterations = doc.select("table td:eq(1)");
                    Elements eDataLearned = doc.select("table td:eq(2)");
                    Elements eConfidence = doc.select("table td:eq(3)");

                    for (int ii = 1; ii < eNames.size() - 1; ii++) {
                        JSONObject jInstance = new JSONObject();

                        jInstance.put(KB.INSTANCE_NAME, eNames.get(ii).text());

                        String link = "http://rtw.ml.cmu.edu/rtw/kbbrowser/entity.php?id=" + jCategory.get(KB.CATEGORY_NAME) + ":" + jInstance.get(KB.INSTANCE_NAME);
                        jInstance.put(KB.LINK, link);

                        jInstance.put(KB.INSTANCE_ITERATION, eIterations.get(ii).text());

                        jInstance.put(KB.INSTANCE_DATA_LEARNED, eDataLearned.get(ii - 1).text());

                        jInstance.put(KB.INSTANCE_CONFIDENCE, eConfidence.get(ii - 1).text());

                        jaInstances.add(jInstance);
                    }

                } while (false);

                jCategory.put(KB.CATEGORY_INSTANCES, jaInstances);

            } catch (Exception e) {
                e.printStackTrace();
            }
            //Retornar o resultado
            return jCategory;

        } else {
            System.out.println("[ZENODOTUS] Category " + pred + " not found!");
            return new JSONObject();
        }

    }

    /**
     * Busca resultados de categorias a partir de uma string de busca
     *
     * @param keyword
     * @return
     */
    public JSONArray searchCategory(String keyword) {
        JSONArray results = new JSONArray();

        /**
         * Buscar Correspondência exata
         */
        JSONObject exactMatch = getCategory(keyword);

        if (!exactMatch.isEmpty()) {
            results.add(exactMatch);
        }
        /**
         * Carregar todas as categorias
         */
        JSONArray jaCategories = getCategoriesList();

        JSONObject jCategory = new JSONObject();
        int ic = 0;

        while (ic < jaCategories.size()) {
            jCategory = (JSONObject) jaCategories.get(ic);

            /**
             * Buscar Correspondências Que contêm
             */
            if ((keyword.toLowerCase().contains(jCategory.get(KB.CATEGORY_NAME).toString())
                    || jCategory.get(KB.CATEGORY_NAME).toString().toLowerCase().contains(keyword))
                    && !exactMatch.equals(jCategory)) {

                String pred = jCategory.get(KB.CATEGORY_NAME).toString();
                results.add((JSONObject) getCategory(pred));
            }

            ic++;
        }

        return results;
    }

    public JSONObject getCategoryInstance(String categoryName, String instanceName) {

        /**
         * Verificar se a categoria existe
         */
        JSONObject jCategory = new JSONObject();

        jCategory = getCategory(categoryName);

        if (!jCategory.isEmpty()) {

            /**
             * Verificar se a instância existe
             */
            JSONObject jInstance = new JSONObject();

            JSONArray jaInstances = (JSONArray) jCategory.get(KB.CATEGORY_INSTANCES);
            boolean match = false;
            int i = 0;

            while (!match && i < jaInstances.size()) {

                jInstance = (JSONObject) jaInstances.get(i);
                if (jInstance.get(KB.INSTANCE_NAME).equals(instanceName)) {
                    match = true;
                }
                i++;
            }

            if (match) {

                /**
                 * Carregar Instancia
                 */
                Document doc = null;
                try {
                    
                    boolean conected = false;
            while(!conected){
            try{
                
            doc = Jsoup.connect(jInstance.get(KB.LINK).toString()).get();
            conected = true;
            
            }catch(Exception e){
                
                System.out.println("[ZENODOTUS] No connection! trying again in 3 seconds...");
                e.printStackTrace();
                Thread.sleep(3000);
            }
            
            }
                    
                    
                    /**
                     * Buscando String Literais
                     */

                    Elements eParagrafs = doc.getElementsByTag("p");
                    Element eLiteralStringParagraf = eParagrafs.first();

                    if (eParagrafs.first() != null) {

                        JSONArray jaLiteralStrings = new JSONArray();

                        Elements eAs = eLiteralStringParagraf.getElementsByTag("a");

                        for (int ia = 0; ia < eAs.size(); ia++) {

                            String sLSValue = eAs.get(ia).text();

                            Elements hrefs = eAs.get(ia).getElementsByAttribute("href");
                            Element href = hrefs.first();
                            String sLSLink = href.attr("href");

                            JSONObject jLiteralString = new JSONObject();
                            jLiteralString.put(KB.LINK, sLSLink);
                            jLiteralString.put(KB.LITERAL_STRING_VALUE, sLSValue);

                            jaLiteralStrings.add(jLiteralString);
                        }

                        jInstance.put(KB.INSTANCE_LITERAL_STRINGS, jaLiteralStrings);

                    }

                    JSONObject jICategory = new JSONObject();

                    /**
                     * Buscando Contextos do CPL
                     */
                    Elements eUls = doc.getElementsByTag("ul");
                    match = false;
                    int iu_index = -1;
                    for (int iu = 0; iu < eUls.size() && !match; iu++) {

                        Elements eLis = eUls.get(iu).getElementsByTag("li");

                        if (eLis.size() > 0) {
                            Elements ePredicates = eLis.first().getElementsByAttributeValue("class", "predicate");

                            if (ePredicates.size() > 0) {
                                boolean match_cpl = false;
                                boolean match_seal = false;

                                for (int il = 1; il < eLis.size() && !match; il++) {
                                    /**
                                     * CPL
                                     */

                                    if (eLis.get(il).text().contains("CPL")) {

                                        String cpl = eLis.get(il).text().toString();

                                        int indexA;
                                        int indexB = 0;

                                        String context;

                                        JSONArray jaCPL = new JSONArray();

                                        do {
                                            indexA = cpl.indexOf("\"", indexB + 1);
                                            indexB = cpl.indexOf("\"", indexA + 1);

                                            if (indexA > 0 && indexB > 0) {

                                                context = cpl.substring(indexA + 1, indexB);

                                                jaCPL.add(context);
                                            }

                                        } while (indexA > 0 && indexB > 0);

                                        jICategory.put(KB.INSTANCE_CATEGORY_CPL, jaCPL);

                                        match_cpl = true;
                                    }
                                    /**
                                     * SEAL
                                     */

                                    if (eLis.get(il).text().contains("SEAL")) {
                                        Elements eLi_as = eLis.get(il).getElementsByTag("a");
                                        if (eLi_as.size() > 0) {
                                            JSONArray jSealEvidences = new JSONArray();
                                            for (int ia = 0; ia < eLi_as.size(); ia++) {
                                                String link = eLi_as.get(ia).attr("href");
                                                jSealEvidences.add(link);
                                            }

                                            jICategory.put(KB.INSTANCE_CATEGORY_SEAL, jSealEvidences);
                                        }
                                        match_seal = true;
                                    }

                                    if (match_cpl && match_seal) {
                                        match = true;
                                    }

                                }
                                match = true;
                                iu_index = iu + 1;
                            }

                            /**
                             * Inserindo Categoria
                             */
                            jICategory.put(KB.CATEGORY_PROLOGUE, jCategory.get(KB.CATEGORY_PROLOGUE).toString());
                            jICategory.put(KB.LINK, jCategory.get(KB.LINK).toString());
                            jICategory.put(KB.CATEGORY_NAME, jCategory.get(KB.CATEGORY_NAME).toString());

                            jInstance.put(KB.INSTANCE_CATEGORY, jICategory);

                        }

                    }//fim ui

                    /**
                     * Buscando Relações
                     */
                    Elements ePredicates = doc.getElementsByAttributeValue("class", "predicate");

                    JSONArray jaRelations = new JSONArray();

                    for (int ip = 1; ip < ePredicates.size(); ip++) {
                        String text = ePredicates.get(ip).text().toString();
                        String name = text;

                        int parentheses_index = text.indexOf("(");

                        if (parentheses_index > 0) {
                            name = text.substring(0, parentheses_index);
                        }

                        Elements hrefs = ePredicates.get(ip).getElementsByAttribute("href");
                        Element href = hrefs.first();
                        String link = KNOWLEDGE_BROWSER_URL + href.attr("href").substring(1);

                        JSONObject jRelation = new JSONObject();
                        jRelation.put(KB.RELATION_NAME, name);
                        jRelation.put(KB.LINK, link);

                        jaRelations.add(jRelation);
                    }

                    jInstance.put(KB.INSTANCE_RELATIONS, jaRelations);

                    //TODO: Futura melhoria - detalhamento das relações
                    /**
                     * Se você quiser remover os comentários desta parte,
                     * comente a a linha de código acima.
                     */
                    /**
                     * JSONArray jaRelations2 = new JSONArray();
                     *
                     *
                     *
                     * for (int iu = iu_index; iu < eUls.size(); iu++) {
                     * ePredicates = eUls.get(iu).getElementsByAttributeValue("class", "predicate");
                     * Elements eSrcs = eUls.get(iu).getElementsByAttributeValue("class", "src");
                     * if (ePredicates.size() > 0 && eSrcs.size() > 0) {
                     * JSONObject jRelation = new JSONObject(); for (int ips =
                     * 0; ips < ePredicates.size() && ips < eSrcs.size(); ips++)
                     * { String name = ePredicates.get(ips).text().toString();
                     *
                     * Elements hrefs =
                     * ePredicates.get(ips).getElementsByAttribute("href");
                     * Element href = hrefs.first(); String link =
                     * KNOWLEDGE_BROWSER_URL + href.attr("href").substring(1);
                     *
                     * System.out.println(name); System.out.println(link);
                     *
                     * jRelation.put(KB.RELATION_NAME, name);
                     * jRelation.put(KB.LINK, link);
                     *
                     * // System.out.println(eSrcs.get(ips)); Elements eLis =
                     * eSrcs.get(ips).getElementsByTag("li");
                     *
                     * for (int il = 0; il < eLis.size(); il++) {
                     * String content = eLis.get(il).text();
                     * if(content.contains("OE")){
                     * int index_firt_parentheses = content.indexOf(")");
                     * int index1 = content.indexOf("(", index_firt_parentheses+1);
                     * int index2 = content.indexOf(",",index1+1);
                     * String arg1 = content.substring(index1+1,index2);
                     * index1 = content.indexOf(")", index2);
                     * String arg2 = content.substring(index2+1, index1);
                     *
                     * jRelation.put(KB.INSTANCE_RELATION_ARG1, arg1);
                     * jRelation.put(KB.INSTANCE_RELATION_ARG2, arg2);
                     *
                     *
                     * //Continuar daqui
                     *
                     * System.out.println(content);
                     * }
                     *
                     *
                     * // CPL
                     *
                     * if (content.contains("CPL")) {
                     * int indexA;
                     * int indexB = 0;
                     *
                     * String context;
                     *
                     * JSONArray jaCPL = new JSONArray();
                     *
                     * do {
                     * indexA = content.indexOf("\"", indexB + 1);
                     * indexB = content.indexOf("\"", indexA + 1);
                     *
                     * if (indexA > 0 && indexB > 0) {
                     *
                     * context = content.substring(indexA + 1, indexB);
                     *
                     * jaCPL.add(context); }
                     *
                     * } while (indexA > 0 && indexB > 0);
                     *
                     * jRelation.put(KB.INSTANCE_RELATION_CPL, jaCPL);
                     *
                     * }
                     * /SEAL if (content.contains("SEAL")) { Elements eLi_as =
                     * eLis.get(il).getElementsByTag("a"); if (eLi_as.size() >
                     * 0) { JSONArray jSealEvidences = new JSONArray(); for (int
                     * ia = 0; ia < eLi_as.size(); ia++) { String rslink =
                     * eLi_as.get(ia).attr("href"); jSealEvidences.add(rslink);
                     * }
                     *
                     * jRelation.put(KB.INSTANCE_RELATION_SEAL, jSealEvidences);
                     * } } }
                     *
                     * }
                     *
                     * jaRelations2.add(jRelation);
                     *
                     * }
                     *
                     * }
                     *
                     * jInstance.put(KB.INSTANCE_RELATIONS, jaRelations2);
                     *
                     *
                     */
                    /**
                     *
                     */
                } catch (Exception e) {
                    e.printStackTrace();
                }

                /**
                 * Retornar Resultado
                 */
                return jInstance;

            } else {
                System.out.println("[ZENODOTUS] Instance " + instanceName + " not found!");
                return null;
            }

        } else {
            System.out.println("[ZENODOTUS] Category " + categoryName + " not found!");
            return null;
        }

    }

    public JSONArray searchCategoryInstance(String categoryName, String keyword) {
        JSONArray results = new JSONArray();

        JSONObject jCategory = getCategory(categoryName);
        if (!jCategory.isEmpty()) {
            JSONArray jInstances = (JSONArray) jCategory.get(KB.CATEGORY_INSTANCES);

            int ii = 0;
            while (ii < jInstances.size()) {
                boolean match = false;
                JSONObject jInstance = (JSONObject) jInstances.get(ii);

                String name = (String) jInstance.get(KB.INSTANCE_NAME);

                if (name.equals(keyword) || (name.contains(keyword))) {
                    String instance_name = (String) jInstance.get(KB.INSTANCE_NAME);

                    results.add(getCategoryInstance(categoryName, instance_name));
                    match = true;
                }
                if (!match) {
                    String instance_name = (String) jInstance.get(KB.INSTANCE_NAME);
                    JSONObject jCompleteInstance = (JSONObject) getCategoryInstance(categoryName, instance_name);

                    if (jCompleteInstance.containsKey(KB.INSTANCE_LITERAL_STRINGS)) {
                        JSONArray literalStrings = (JSONArray) jCompleteInstance.get(KB.INSTANCE_LITERAL_STRINGS);
                        if (literalStrings.size() > 0) {
                            for (int ils = 0; ils < literalStrings.size() && !match; ils++) {
                                JSONObject jliteralString = (JSONObject) literalStrings.get(ils);
                                String literalString = (String) jliteralString.get(KB.LITERAL_STRING_VALUE);

                                if (literalString.equals(keyword) || (literalString.contains(keyword))) {
                                    results.add(jCompleteInstance);
                                    match = true;
                                }
                            }
                        }
                    }
                }

                ii++;
            }

        } else {
            System.out.println("[ZENODOTUS] Category " + categoryName + " not found!");
        }

        return results;
    }

    public JSONArray searchCategoryInstance(String keyword) {
        JSONArray results = new JSONArray();

        JSONArray jCategories = getCategoriesList();

        int ic = 0;

        while (ic < jCategories.size()) {
            JSONObject jCategory = (JSONObject) jCategories.get(ic);
            String pred = (String) jCategory.get(KB.CATEGORY_NAME);
            
            System.out.println("[SCI] Searching in "+pred+" ("+(ic+1)+" of "+jCategories.size()+")...");
            int or = results.size();
            results.addAll(searchCategoryInstance(pred, keyword));
            System.out.println("[SCI] "+(results.size()-or)+" new instances.");
            ic++;
        }
        System.out.println("[SCI] "+results.size()+" instances found.");
        return results;
    }

    public JSONArray getRelationsList() {
        JSONArray result = new JSONArray();
        Document doc = null;
        try {
            boolean conected = false;
            while (!conected) {
                try {

                    //Carregando a página
                    doc = Jsoup.connect(KNOWLEDGE_BROWSER_URL + "ontology.php?mode=rel").get();
                    conected = true;

                } catch (Exception e) {

                    System.out.println("[ZENODOTUS] No connection to get categories! trying again in 3 seconds...");
                    e.printStackTrace();
                    Thread.sleep(3000);
                }

            }

            //Primeira seleção de elementos pela tag "li"    
            Elements elements = doc.getElementsByTag("li");

            //Percorrendo todos os elementos encontrados
            for (int ie = 0; ie < elements.size(); ie++) {

                //Selecionando apenas os elementos com a classe "name" (apenas as categorias)
                Elements eName = elements.get(ie).getElementsByAttribute("name");
                if (eName.size() > 0) {
                    //Armazenando o nome e link de cada categoria em um JSONObject
                    Elements href = elements.get(ie).getElementsByAttribute("href");

                    JSONObject jRelation = new JSONObject();
                    jRelation.put(KB.RELATION_NAME, eName.first().attr("name"));
                    String link = "http://rtw.ml.cmu.edu/rtw/kbbrowser/list.php?pred=" + jRelation.get(KB.RELATION_NAME).toString();
                    jRelation.put(KB.LINK, link);

                    //Adicionando a categoria ao resultado
                    result.add(jRelation);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public JSONObject getRelation(String pred) {
        JSONObject jRelation = new JSONObject();

        //Verificar se o nome da categoria existe
        JSONArray jaRelations = getRelationsList();

        int ir = 0;
        boolean match = false;

        while (!match && ir < jaRelations.size()) {
            jRelation = (JSONObject) jaRelations.get(ir);

            if (pred.equals(jRelation.get(KB.RELATION_NAME))) {
                match = true;
            }

            ir++;
        }

        if (match) {

            //Carregar página da categoria
            Document doc = null;
            try {
                //TODO: Passar url por arquivo de setup
                boolean conected = false;
                while (!conected) {
                    try {

                        //Carregando a página   
                        doc = Jsoup.connect(jRelation.get(KB.LINK).toString()).timeout(30000).get();
                        conected = true;

                    } catch (Exception e) {

                        System.out.println("[ZENODOTUS] No connection to get relations! trying again in 3 seconds...");
                        e.printStackTrace();
                        Thread.sleep(3000);
                    }

                }

                //Coletar dados e inserir em um JSONObject
                //Prologo
                Elements ePrologue_r = doc.getElementsByAttributeValue("class", "prologue_r");
                if (ePrologue_r.size() > 0) {
                    jRelation.put(KB.RELATION_PROLOGUE, ePrologue_r.first().text());
                }
                //Domain and Range
                Elements ePrologue_l = doc.getElementsByAttributeValue("class", "prologue_l");
                if (ePrologue_l.size() > 0) {
                    Elements eAs = ePrologue_l.first().getElementsByAttribute("href");
                    //Domain
                    String sDomainLink = KNOWLEDGE_BROWSER_URL + eAs.get(0).attr("href").substring(2);
                    String sDomainName = eAs.get(0).text();

                    JSONObject jDomain = new JSONObject();
                    jDomain.put(KB.CATEGORY_NAME, sDomainName);
                    jDomain.put(KB.LINK, sDomainLink);

                    jRelation.put(KB.RELATION_DOMAIN, jDomain);

                    //Range
                    String sRangeLink = KNOWLEDGE_BROWSER_URL + eAs.get(1).attr("href").substring(2);
                    String sRangeName = eAs.get(1).text();

                    JSONObject jRange = new JSONObject();
                    jRange.put(KB.CATEGORY_NAME, sRangeName);
                    jRange.put(KB.LINK, sRangeLink);

                    jRelation.put(KB.RELATION_RANGE, jRange);
                }

                //Instâncias
                JSONArray jaInstances = new JSONArray();
                //TODO: Não está carregando toda a página, RESOLVER ISSO!!!
                //TODO: pode existir mais de uma página com resultados, fazer percorrer todas
                do {
                    Elements eInstances = doc.getElementsByAttributeValue("class", "instance");
                    //System.out.println(eInstances);

                    Elements eArgs = doc.select("table td:eq(0)");
                    Elements eIterations = doc.select("table td:eq(1)");
                    Elements eDataLearned = doc.select("table td:eq(2)");
                    Elements eConfidence = doc.select("table td:eq(3)");

                    for (int ii = 1; ii < eArgs.size() - 1; ii++) {
                        JSONObject jInstance = new JSONObject();

                        String sArgs = eArgs.get(ii).text();

                        int index_comma = sArgs.indexOf(",");
                        String sArg1 = sArgs.substring(0, index_comma);
                        String sArg2 = sArgs.substring(index_comma + 2);
                        jInstance.put(KB.INSTANCE_ARG1, sArg1);
                        jInstance.put(KB.INSTANCE_ARG2, sArg2);

                        jInstance.put(KB.INSTANCE_ITERATION, eIterations.get(ii).text());

                        jInstance.put(KB.INSTANCE_DATA_LEARNED, eDataLearned.get(ii - 1).text());

                        jInstance.put(KB.INSTANCE_CONFIDENCE, eConfidence.get(ii - 1).text());

                        jaInstances.add(jInstance);
                    }

                } while (false);

                jRelation.put(KB.RELATION_INSTANCES, jaInstances);

            } catch (Exception e) {
                e.printStackTrace();
            }
            //Retornar o resultado
            return jRelation;

        } else {
            System.out.println("[ZENEDOTUS] Relation " + pred + " not found!");
            return new JSONObject();
        }

    }

    public JSONArray searchRelation(String keyword) {
        JSONArray results = new JSONArray();

        /**
         * Buscar Correspondência exata
         */
        JSONObject exactMatch = getRelation(keyword);

        if (!exactMatch.isEmpty()) {
            results.add(exactMatch);
        }
        /**
         * Carregar todas as categorias
         */
        JSONArray jaRelations = getRelationsList();

        JSONObject jRelation = new JSONObject();
        int ir = 0;

        while (ir < jaRelations.size()) {
            jRelation = (JSONObject) jaRelations.get(ir);

            /**
             * Buscar Correspondências Que contêm
             */
            if ((keyword.toLowerCase().contains(jRelation.get(KB.RELATION_NAME).toString())
                    || jRelation.get(KB.RELATION_NAME).toString().toLowerCase().contains(keyword))
                    && !exactMatch.equals(jRelation)) {

                String pred = jRelation.get(KB.RELATION_NAME).toString();
                results.add((JSONObject) getRelation(pred));
            }

            ir++;
        }

        return results;
    }

}
