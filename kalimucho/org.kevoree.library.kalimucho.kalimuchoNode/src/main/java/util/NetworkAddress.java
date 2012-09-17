package util;

/**
 * Syntax analyser for network addresses
 * This class is constructed with a String to analyse
 * If this string is a valid network address some informations are provided:<br>
 *  - type of address (IPv4, IPv6, Bluetooth, IEEE)<br>
 *  - address in a extended form (all parts filled with 0)<br>
 *  - address in a reduced form (all parts without zeros)<br>
 *  - address in a normalized form
 * (it is difficult to say is a normalized form for an address because different OS use different representations)<br>
 * This class can also be used to compare two addresses (method equals)<br>
 *
 * The accepted syntax are the following:<br>
 *  - For Bluetooth: a maximum of 12 hexadecimal digits or 6 parts of 1 to 2 hexadecimal digits separated by :<br>
 *  - For IEEE: 4 part of 4 hexadecimal digits separated by .<br>
 *  - For IPv4: 4 part of 1 to 3 decimal digits (each part representing an integer from 0 to 255) separated by .<br>
 *  - For IPv6: 8 part of 1 to 2 hexadecimal digits separated by :<br>
 *      only one part can by empty (replaced by 0)<br>
 *      a zone indice can terminate the string: it starts with % and holds only decimal digits<br>
 *
 * @author Dalmau
 */

public class NetworkAddress {

    // Constantes correspondant aux types d'adresses connus
    /**
     * Constant used to qualify an IPv4 address
     */
    public static final int TYPE_IPV4 = 4;
    /**
     * Constant used to qualify an IPv6 address
     */
    public static final int TYPE_IPV6 = 6;
    /**
     * Constant used to qualify an IEEE (Zigbee) address
     */
    public static final int TYPE_IEEE = 3;
    /**
     * Constant used to qualify a bluetooth address
     */
    public static final int TYPE_BLUETOOTH = 2;
    /**
     * Constant used to qualify an unknown type address
     */
    public static final int TYPE_UNKNOWN = -1;

    // Constantes utilisisees pour decouper les adresses
    private static final int TAILLE_IPV4 = 4; // IPV4 = 4 parties decimales separees par des .
    private static final int TAILLE_IPV6 = 8; // IPV8 = 8 parties separees par des :
    private static final int TAILLE_IEEE = 4; // IEEE = 4 parties hexadecimales de taiile 4 separees par des .
    private static final int TAILLE_BLUETOOTH = 12; // Bluetooth = 12 chiffres hexadecimaux au maximum
    private static final int GROUPES_BLUETOOTH = 6; // Bluetooth = 6 parties hexadecimales de 2 separes par des :
    private static final char separateurIPv4 = '.'; // separation des groupes en IPv4
    private static final char separateurIPv6 = ':'; // separation des groupes en IPv6
    private static final char separateurIEEE = '.'; // separation des groupes en IEEE
    private static final char separateurZoneIPv6 = '%'; // separation de l'indice de zone en IPv6

    // Proprietes liees a une addresse apres analyse
    private int type; // type d'adresse = l'une des constantes definies ci-dessus
    private String reducedAddress; // adresse sans 0 au debut des champs (sauf pour IEEE)
    private String extendedAddress; // adresse avec chaque champ complete avec des 0
    private String indiceDeZone; // indice de zone uniquement en IPv6 voir RFC4007 paragraphe 6

    // Methodes publiques

    /**
     * Creates a network address from a string
     * @param adresse the string containing the address
     */
    public NetworkAddress(String adresse) { // construction d'une adresse a partir d'une chaine
        type = addressType(adresse);
        reducedAddress = reduitAdresse(type, adresse);
        extendedAddress = etendAdresse(type, adresse);
        if (type == TYPE_IPV6) {
            int pos = adresse.indexOf("%");
            if (pos != -1) indiceDeZone = adresse.substring(pos);
            else indiceDeZone = "";
        }
        else indiceDeZone = "";
    }

    /**
     * Returns the type of address (IPv4, IPv6 ...)
     * @return the type of address (IPv4, IPv6 ...)
     */
    public int getType() { return type; } // renvoie le type de l'adresse

    /**
     * Returns a string containing the address without leading zeros
     * The indice zone part is removed for IPv6 (see getZoneIndice())
     * @return a string containing the address without leading zeros
     */
    public String getReducedAddress() { return reducedAddress; } // renvoie l'adresse sans 0 en debit de champ (sauf pour IEEE)

    /**
     * Returns a string containing the address with all parts completed to full size by zeros
     * The indice zone part is removed for IPv6 (see getZoneIndice())
     * @return a string containing the address with all parts completed to full size by zeros
     */
    public String getExtendedAddress() { return extendedAddress; } // renvoie l'adresse avec les champs completes a leur taille max

    /**
     *  Return a string containing the address in a format usable on network
     * The indice zone part is removed for IPv6 (see getZoneIndice())
     * @return a string containing the address in a format usable on network
     */
    public String getNormalizedAddress() {
        switch(type) {
            case TYPE_IPV4 : return reducedAddress;
            case TYPE_IPV6 : return extendedAddress;
            case TYPE_IEEE : return extendedAddress;
            case TYPE_BLUETOOTH : return extendedAddress;
            default : return extendedAddress;        }
    }

    /**
     * Returns the zone indice of an IPv6 address or an empty string for non IPv6 addresses
     * @return the zone indice of an IPv6 address or an empty string for non IPv6 addresses
     */
    public String getZoneIndice() { return indiceDeZone; } // renvoie l'indice de zone (valable uniquement pour IPv6)

    /**
     * Compares two network addresses
     * For IPv6 addresses the zone indice part is not compared
     * @param autre the network address to compare
     * @return true if same address false otherwise
     */
    public boolean equals(NetworkAddress autre) { // compare 2 adresses
        if (autre == null) return false;
        if (type != autre.getType()) return false;
        else {
            return extendedAddress.equals(autre.getExtendedAddress());
        }
    }

    /**
     * Compare the type of two addresses
     * @param autre the network address to compare
     * @return true if same typa (IPv4, IPv6 ...) false otherwise
     */
    public boolean isSameType(NetworkAddress autre) {
        return (type == autre.getType());
    }

    /**
     * Return true if the address is of an unknown type
     * @return true if the address is of an unknown type false otherwise
     */
    public boolean isUnknown() {
        return (type == TYPE_UNKNOWN);
    }

    /**
     * Return true if the address is of a known type false otherwise
     * @return true if the address is of a known type false otherwise
     */
    public boolean isKnown() {
        return (type != TYPE_UNKNOWN);
    }

    /**
     * Returns true if the address is IPv4 false otherwise
     * @return true if the address is IPv4 false otherwise
     */
    public boolean isIPv4() {
        return (type == TYPE_IPV4);
    }

    /**
     * Returns true if the address is IPv6 false otherwise
     * @return true if the address is IPv6 false otherwise
     */
    public boolean isIPv6() {
        return (type == TYPE_IPV6);
    }

    /**
     * Returns true if the address is IEEE (Zigbee) false otherwise
     * @return true if the address is IEEE (Zigbee) false otherwise
     */
    public boolean isIEEE() {
        return (type == TYPE_IEEE);
    }

    /**
     * Returns true if the address is bluetooth false otherwise
     * @return true if the address is bluetooth false otherwise
     */
    public boolean isBluetooth() {
        return (type == TYPE_BLUETOOTH);
    }

    // Methodes privees

    //  Enleve les 0 de debut de champ d'une adresse quelconque
    private String reduitAdresse(int typ, String adresse) {
        switch (typ) {
            case TYPE_IPV4:
                return reduitIPv4(adresse);
            case TYPE_IPV6:
                return reduitIPv6(adresse);
            case TYPE_BLUETOOTH:
                return reduitBluetooth(adresse);
            case TYPE_IEEE:
                return adresse;
            default :
                return adresse;
        }
    }

    // Complete les champs d'une adresse quelconque a leur taille maximale
    private String etendAdresse(int typ, String adresse) { 
        switch (typ) {
            case TYPE_IPV4:
                return etendIPv4(adresse);
            case TYPE_IPV6:
                return etendIPv6(adresse);
            case TYPE_BLUETOOTH:
                return etendBluetooth(adresse);
            case TYPE_IEEE:
                return adresse;
            default :
                return adresse;
        }
    }

    // Analyse une adresse pour en determiner le type
    private int addressType(String adresse) {
        int etat = 0; // etat de l'automate d'analyse de l'adresse (en sortie de boucle c'est l'etat final)
        int compteV4 =0; // compte des champs decouverts en IPv4
        int compteV6 =0; // compte des champs decouverts en IPv6
        int compteIEEE =0; // compte des champs decouverts en IEEE
        int etatAtteint = TYPE_UNKNOWN; // dernier etat de l'automate dans lequel un type d'adresse a ete reconnu
        String partieIPV4 = ""; // champs en IPv4 pour verifier que la valeur soit entre 0 et 255
        int compteVide = 0; // compte des champs vides en IPv6 (un seul est autorise dans une adresse)

        int i = 0; // indice de parcours de la chaine
        boolean erreur = false; // sortie de l'automate en cas d'erreur de syntaxe de la chaine
        while ((i<adresse.length()) && (!erreur)) { // jusqu'a la fin de la chaine ou erreur
            char c = adresse.charAt(i); // caractere courant
            switch (etat) { // automate selon l'etat actuel
                case 0: // etat initial
                    if (isDecimal(c)) { // lecture d'un decimal
                        partieIPV4 = partieIPV4+c;
                        etat = 1;
                    }
                    else {
                        if (isHexadecimal(c)) etat = 4; // lecture d'un hexadecimal
                            else {
                                if (c==separateurIPv6) { // lecture d'un champ vide
                                    etat = 16; // a priori on a reconnu du IPV6
                                    compteVide++;
                                }
                                else {
                                    if ((c==separateurZoneIPv6) && (etatAtteint == 16) && (compteV6 == TAILLE_IPV6-1)) etat = 8; // indice de zone
                                    else erreur = true;
                                }
                            }
                    }
                    break;
                case 1: // etat apres lecture d'1 chiffre decimal
                    if (isDecimal(c)) { // lecture d'un decimal
                        etat = 2;
                        partieIPV4 = partieIPV4+c;
                    }
                    else {
                        if (isHexadecimal(c)) etat = 5; // lecture d'un hexadecimal
                        else {
                            if (c==separateurIPv4) etat = 14; // fin de champ par .  a priori on a reconnu du IPV4
                            else {
                                if (c==separateurIPv6) etat = 16; // fin de champ par : a priori on a reconnu du IPV6
                                else {
                                    if ((c==separateurZoneIPv6) && (etatAtteint == 16) && (compteV6 == TAILLE_IPV6-1)) etat = 8; // indice de zone
                                    else erreur = true;
                                }
                            }
                        }
                    }
                    break;
                case 2: // etat apres lecture de 2 chiffres decimaux
                    if (isDecimal(c)) { // lecture d'un decimal
                        etat = 3;
                        partieIPV4 = partieIPV4+c;
                    }
                    else {
                        if (isHexadecimal(c)) etat = 6; // lecture d'un hexadecimal
                        else {
                            if (c==separateurIPv4) etat = 14; // fin de champ par . a priori on a reconnu du IPV4
                            else {
                                if (c==separateurIPv6) etat = 16; // fin de champ par : a priori on a reconnu du IPV6
                                else {
                                    if ((c==separateurZoneIPv6) && (etatAtteint == 16) && (compteV6 == TAILLE_IPV6-1)) etat = 8; // indice de zone
                                    else erreur = true;
                                }
                            }
                        }
                    }
                    break;
                case 3: // etat apres lecture de 3 chiffres decimaux
                    if (c==separateurIPv4) etat = 14; // fin de champ par . a priori on a reconnu du IPV4
                    else {
                        if (isHexadecimal(c)) etat = 7; // lecture d'un hexadecimal
                        else erreur = true;
                    }
                    break;
                case 4: // etat apres lecture d'1 chiffre hexadecimal
                    if (isHexadecimal(c)) etat = 5; // lecture d'un hexadecimal
                    else {
                        if (c==separateurIPv6) etat = 16; // fin de champ par :  a priori on a reconnu du IPV6
                        else {
                            if ((c==separateurZoneIPv6) && (etatAtteint == 16) && (compteV6 == TAILLE_IPV6-1)) etat = 8; // indice de zone
                            else erreur = true;
                        }
                    }
                    break;
                case 5: // etat apres lecture de 2 chiffres hexadecimaux
                    if (isHexadecimal(c)) etat = 6; // lecture d'un hexadecimal
                    else {
                        if (c==separateurIPv6) etat = 16; // fin de champ par : a priori on a reconnu du IPV6
                        else {
                            if ((c==separateurZoneIPv6) && (etatAtteint == 16) && (compteV6 == TAILLE_IPV6-1)) etat = 8; // indice de zone
                            else erreur = true;
                        }
                    }
                    break;
                case 6: // etat apres lecture de 3 chiffres hexadecimaux
                    if (isHexadecimal(c)) etat = 7; // lecture d'un hexadecimal
                    else erreur = true;
                    break;
                case 7: // etat apres lecture de 4 chiffres hexadecimaux
                    if (isHexadecimal(c)) { // lecture d'un hexadecimal
                        etat = 11; // a priori on a reconnu du Bluetooth
                        if ((etatAtteint == TYPE_UNKNOWN) || (etatAtteint == etat)) etatAtteint = etat;
                        else erreur = true; // on ne peut pas changer de type
                    }
                    else {
                        if (c==separateurIEEE) etat = 12; // fin de champ par . a priori on a reconnu du IEEE
                        else erreur = true;
                    }
                    break;
                case 8 : // etat correspondant a la lecture de l'indice de zone en IPv6
                    if (!isDecimal(c)) erreur = true; // on n'accepte que des decimaux en indice de zone
                    break;
                }
            switch (etat) {
                case 14: // etat apres decouverte d'au moins un champ IPv4
                    if ((etatAtteint == TYPE_UNKNOWN) || (etatAtteint == etat)) { // on n'avait pas reconnu autre chose avant
                        etatAtteint = etat;
                        compteV4++; // compter un champ IPv4 de plus
                        if (compteV4 == TAILLE_IPV4) erreur = true; // trop de champs
                        else {
                            if (testePartieIPV4(partieIPV4)) { // le champ est bien entre 0 et 2555
                                partieIPV4=""; // pour tester le champ suivant
                                etat = 0;
                            }
                            else erreur = true; // champ incorrect (hors de 0...255)
                        }
                    }
                    else erreur = true; // on ne peut pas changer de type
                    break;
                case 16: // etat apres decouverte d'au moins un champ IPv6
                    if ((etatAtteint == TYPE_UNKNOWN) || (etatAtteint == etat)) { // on n'avait pas reconnu autre chose avant
                        etatAtteint = etat;
                        compteV6++; // compter un champ IPv6 de plus
                        if (compteV6 == TAILLE_IPV6) erreur = true; // trop de champs
                        else etat = 0;
                    }
                    else erreur = true; // on ne peut pas changer de type
                    break;
                case 12: // etat apres decouverte d'au moins un champ IEEE
                    if ((etatAtteint == TYPE_UNKNOWN) || (etatAtteint == etat)) { // on n'avait pas reconnu autre chose avant
                        etatAtteint = etat;
                        compteIEEE++; // compter un champ IEEE de plus
                        if (compteIEEE == TAILLE_IEEE) erreur = true; // trop de champs
                        else etat = 0;
                    }
                    else erreur = true; // on ne peut pas changer de type
                    break;
                case 11:  // etat apres decouverte d'une chaine pouvant correspondre a du bluetooth
                    if (!isHexadecimal(c)) erreur = true; // on n'accepte que de l'hexadecimal
                    break;
            }
            if (!erreur) i++; // s'il n'y a pas eu d'erreur continuer l'analyse de la chaine
        }
        // la boucle d'analyse de la chaine est terminee
        if (erreur) return TYPE_UNKNOWN; // s'il y a eu une erreur l'automate n'a rien reconnu
        else { // l'automate a reconnu un type mais il peut etre de format incorrect
            switch (etatAtteint) { // terminer l'analyse en fonction du type que l'on pense avoir reconnu
                case 11 : // probablement du bluetooth => verifier la taille
                    if (adresse.length() <= TAILLE_BLUETOOTH) return TYPE_BLUETOOTH;
                    else return TYPE_UNKNOWN;
                case 12 : // probablement du IEEE => verifier l'etat final de l'automate et le nombre de champs
                    if ((compteIEEE == TAILLE_IEEE-1) && (etat == 7)) return TYPE_IEEE;
                    else return TYPE_UNKNOWN;
                case 14 : // probablement du IPv4 => verifier le nombre de champs et la validite (0 a 255) du dernier
                    if ((compteV4 == TAILLE_IPV4-1) && ((etat == 1) || (etat == 2) || (etat == 3))) {
                        if (testePartieIPV4(partieIPV4)) return TYPE_IPV4;
                        else return TYPE_UNKNOWN;
                    }
                    else return TYPE_UNKNOWN;
                case 16 : // probablement du IPv6 => verifier l'etat final de l'automate, le nombre de champs et le nombre de champs vides
                    if ((compteV6 == TAILLE_IPV6-1) && (compteVide <=1) && ((etat == 1) || (etat == 2) || (etat == 4) || (etat == 5) || (etat == 8))) return TYPE_IPV6;
                    else if ((compteV6 == GROUPES_BLUETOOTH-1) && (compteVide ==0) && ((etat == 1) || (etat == 2) || (etat == 4) || (etat == 5))) return TYPE_BLUETOOTH;
                         else return TYPE_UNKNOWN;
                default : 
                    if (((etat == 1) || (etat == 2) || (etat == 3) || (etat == 4) || (etat == 5) || (etat == 7)) && (adresse.length() <= TAILLE_BLUETOOTH)) {
                        return TYPE_BLUETOOTH;
                    }
                    else return TYPE_UNKNOWN;
            }
        }
    }

    // teste les chiffres decimaux
    private boolean isDecimal(char c) { 
        if ((c=='0')||(c=='1')||(c=='2')||(c=='3')||(c=='4')||(c=='5')||(c=='6')||(c=='7')||(c=='8')||(c=='9')) return true;
        else return false;
    }

    // teste les caracteres hexadecimaux
    private boolean isHexadecimal(char c) { 
        if (isDecimal(c)) return true;
        else {
            if ((c=='A')||(c=='B')||(c=='C')||(c=='D')||(c=='E')||(c=='F')||(c=='a')||(c=='b')||(c=='c')||(c=='d')||(c=='e')||(c=='f')) return true;
            else return false;
        }
    }

    // teste qu'un champ IPv4 soit bien entre 0 et 255
    private boolean testePartieIPV4(String partie) { 
        int x = -1;
        try { x = Integer.parseInt(partie); }
        catch (NumberFormatException nfe) { return false; }
        if ((x>=0) && (x<256)) return true;
        else return false;
    }

    // enleve les 0 de debut de champs en IPv4
    private String reduitIPv4(String adr) { 
        if (adr == null) return null;
        int pos1, pos2=-1, pos3=-1;
        pos1 = adr.indexOf(separateurIPv4);
        if (pos1 != -1) pos2 = adr.indexOf(separateurIPv4,pos1+1);
        if (pos2 != -1)  pos3 = adr.indexOf(separateurIPv4,pos2+1);
        String p1 = adr.substring(0,pos1);
        String p2 = adr.substring(pos1+1,pos2);
        String p3 = adr.substring(pos2+1,pos3);
        String p4 = adr.substring(pos3+1);
        return (enleveZeros(p1)+"."+enleveZeros(p2)+"."+enleveZeros(p3)+"."+enleveZeros(p4));
    }
	
    // enleve les 0 de debut de champs en IPv6
    private String reduitIPv6(String adr) { 
        if (adr == null) return null;
        int pos1, pos2=-1, pos3=-1, pos4=-1, pos5=-1, pos6=-1, pos7=-1, pos8 = -1;
        pos1 = adr.indexOf(separateurIPv6);
        if (pos1 != -1) pos2 = adr.indexOf(separateurIPv6,pos1+1);
        if (pos2 != -1)  pos3 = adr.indexOf(separateurIPv6,pos2+1);
        if (pos3 != -1)  pos4 = adr.indexOf(separateurIPv6,pos3+1);
        if (pos4 != -1)  pos5 = adr.indexOf(separateurIPv6,pos4+1);
        if (pos5 != -1)  pos6 = adr.indexOf(separateurIPv6,pos5+1);
        if (pos6 != -1)  pos7 = adr.indexOf(separateurIPv6,pos6+1);
        if (pos7 != -1)  pos8 = adr.indexOf(separateurZoneIPv6,pos7+1);
        String p1 = adr.substring(0,pos1);
        String p2 = adr.substring(pos1+1,pos2);
        String p3 = adr.substring(pos2+1,pos3);
        String p4 = adr.substring(pos3+1, pos4);
        String p5 = adr.substring(pos4+1, pos5);
        String p6 = adr.substring(pos5+1, pos6);
        String p7 = adr.substring(pos6+1, pos7);
        String p8;
        if (pos8==-1) p8 = adr.substring(pos7+1);
        else p8 = adr.substring(pos7+1, pos8);
        String resultat = (enleveZeros(p1)+":"+enleveZeros(p2)+":"+enleveZeros(p3)+":"+enleveZeros(p4)+":"+enleveZeros(p5)+":"+enleveZeros(p6)+":"+enleveZeros(p7)+":"+enleveZeros(p8));
        return resultat;
    }

    // enleve les 0 de debut en bluetooth
    private String reduitBluetooth(String adr) { 
        String resultat = "";
        if (!adr.contains(":")) { // adresse bluetooth sans :
            boolean fini = false;
            for (int i = 0; i<adr.length(); i++) {
                char c = adr.charAt(i);
                if (fini) resultat = resultat+c;
                else {
                    if (adr.charAt(i) != '0') {
                        resultat = resultat+c;
                        fini = true;
                    }
                }
            }
            if (resultat.length() == 0) resultat = "0";
        }
        else { // adresse bluetooth avec :
            int pos1, pos2=-1, pos3=-1, pos4=-1, pos5=-1, pos6=-1, pos7=-1, pos8 = -1;
            pos1 = adr.indexOf(separateurIPv6);
            if (pos1 != -1) pos2 = adr.indexOf(separateurIPv6,pos1+1);
            if (pos2 != -1)  pos3 = adr.indexOf(separateurIPv6,pos2+1);
            if (pos3 != -1)  pos4 = adr.indexOf(separateurIPv6,pos3+1);
            if (pos4 != -1)  pos5 = adr.indexOf(separateurIPv6,pos4+1);
            if (pos5 != -1)  pos6 = adr.indexOf(separateurIPv6,pos5+1);
            String p1 = adr.substring(0,pos1);
            String p2 = adr.substring(pos1+1,pos2);
            String p3 = adr.substring(pos2+1,pos3);
            String p4 = adr.substring(pos3+1, pos4);
            String p5 = adr.substring(pos4+1, pos5);
            String p6 = adr.substring(pos5+1);
            resultat = (enleveZeros(p1)+":"+enleveZeros(p2)+":"+enleveZeros(p3)+":"+enleveZeros(p4)+":"+enleveZeros(p5)+":"+enleveZeros(p6));
        }
        return resultat;
    }

    // met tous les champs a 3 chiffres en IPv4
    private String etendIPv4(String adr) { 
        if (adr == null) return null;
        int pos1, pos2=-1, pos3=-1;
        pos1 = adr.indexOf(separateurIPv4);
        if (pos1 != -1) pos2 = adr.indexOf(separateurIPv4,pos1+1);
        if (pos2 != -1)  pos3 = adr.indexOf(separateurIPv4,pos2+1);
        String p1 = adr.substring(0,pos1);
        String p2 = adr.substring(pos1+1,pos2);
        String p3 = adr.substring(pos2+1,pos3);
        String p4 = adr.substring(pos3+1);
        return (etendATroisCaracteres(p1)+"."+etendATroisCaracteres(p2)+"."+etendATroisCaracteres(p3)+"."+etendATroisCaracteres(p4));
    }

    // met tous les champs a 2 chiffres en IPv6
    private String etendIPv6(String adr) { 
        if (adr == null) return null;
        int pos1, pos2=-1, pos3=-1, pos4=-1, pos5=-1, pos6=-1, pos7=-1, pos8 = -1;
        pos1 = adr.indexOf(separateurIPv6);
        if (pos1 != -1) pos2 = adr.indexOf(separateurIPv6,pos1+1);
        if (pos2 != -1)  pos3 = adr.indexOf(separateurIPv6,pos2+1);
        if (pos3 != -1)  pos4 = adr.indexOf(separateurIPv6,pos3+1);
        if (pos4 != -1)  pos5 = adr.indexOf(separateurIPv6,pos4+1);
        if (pos5 != -1)  pos6 = adr.indexOf(separateurIPv6,pos5+1);
        if (pos6 != -1)  pos7 = adr.indexOf(separateurIPv6,pos6+1);
        if (pos7 != -1)  pos8 = adr.indexOf(separateurZoneIPv6,pos7+1);
        String p1 = adr.substring(0,pos1);
        String p2 = adr.substring(pos1+1,pos2);
        String p3 = adr.substring(pos2+1,pos3);
        String p4 = adr.substring(pos3+1, pos4);
        String p5 = adr.substring(pos4+1, pos5);
        String p6 = adr.substring(pos5+1, pos6);
        String p7 = adr.substring(pos6+1, pos7);
        String p8;
        if (pos8==-1) p8 = adr.substring(pos7+1);
        else p8 = adr.substring(pos7+1, pos8);
        String resultat = (etendADeuxCaracteres(p1)+":"+etendADeuxCaracteres(p2)+":"+etendADeuxCaracteres(p3)+":"+etendADeuxCaracteres(p4)+":"+etendADeuxCaracteres(p5)+":"+etendADeuxCaracteres(p6)+":"+etendADeuxCaracteres(p7)+":"+etendADeuxCaracteres(p8));
        return resultat;
    }

    // met l'addresse sur 12 chiffrres en bluetooth
    private String etendBluetooth(String adr) { 
        String resultat = "";
        if (!adr.contains(":")) {
            int lg = TAILLE_BLUETOOTH - adr.length();
            for (int i=0; i<lg; i++) resultat = resultat+"0";
            resultat=resultat+adr;
        }
        else {
            int pos1, pos2=-1, pos3=-1, pos4=-1, pos5=-1, pos6=-1, pos7=-1, pos8 = -1;
            pos1 = adr.indexOf(separateurIPv6);
            if (pos1 != -1) pos2 = adr.indexOf(separateurIPv6,pos1+1);
            if (pos2 != -1)  pos3 = adr.indexOf(separateurIPv6,pos2+1);
            if (pos3 != -1)  pos4 = adr.indexOf(separateurIPv6,pos3+1);
            if (pos4 != -1)  pos5 = adr.indexOf(separateurIPv6,pos4+1);
            if (pos5 != -1)  pos6 = adr.indexOf(separateurIPv6,pos5+1);
            String p1 = adr.substring(0,pos1);
            String p2 = adr.substring(pos1+1,pos2);
            String p3 = adr.substring(pos2+1,pos3);
            String p4 = adr.substring(pos3+1, pos4);
            String p5 = adr.substring(pos4+1, pos5);
            String p6 = adr.substring(pos5+1);
            resultat = (etendADeuxCaracteres(p1)+":"+etendADeuxCaracteres(p2)+":"+etendADeuxCaracteres(p3)+":"+etendADeuxCaracteres(p4)+":"+etendADeuxCaracteres(p5)+":"+etendADeuxCaracteres(p6));
        }
        return resultat;
    }

    // enleve les 0 de debut sur un champs IPv4
    private String enleveZeros(String part) { 
        switch (part.length()) {
            case 3:
                if (part.charAt(0) == '0') {
                    if (part.charAt(1) == '0') return new String(part.substring(2));
                    else return new String(part.substring(1));
                }
                else return part;
            case 2:
                if (part.charAt(0) == '0') return new String(part.substring(1));
                else return part;
            case 1:
                return part;
            case 0:
            	return "0";
            default:
                return part;
        }
    }

    // ramene un champ IPv4 a une longueur de 3
    private String etendATroisCaracteres(String part) { 
        switch (part.length()) {
            case 3:
                return part;
            case 2:
                return "0"+part;
            case 1:
                return "00"+part;
            case 0:
            	return "000";
            default:
                return part;
        }
    }

    // ramene un champ IPv6 a une longueur de 2
    private String etendADeuxCaracteres(String part) { 
        switch (part.length()) {
            case 2:
                return part;
            case 1:
                return "0"+part;
            case 0:
            	return "00";
            default:
                return part;
        }
    }

}