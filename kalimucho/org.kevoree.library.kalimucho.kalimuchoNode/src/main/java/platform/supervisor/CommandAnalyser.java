package platform.supervisor;

import java.util.Vector;

/**
 * This class analyse a command received by the platform for the supervisor.
 * It splits the string into parts (cammand, parameters ...) and verificate that the command is valid.
 * @author Dalmau
 */
public class CommandAnalyser {

    private static final int STANDARD_CHAR = 0;
    private static final char separateur1 = ' ';
    private static final char separateur2 = '\t';
    private static final int SEPARATEUR = 1;
    private static final char debutListe = '[';
    private static final int DEBUT_LISTE = 2;
    private static final char finListe = ']';
    private static final int FIN_LISTE = 3;
    private String[] commande;
    private String[] listeEntrees;
    private String[] listeSorties;
    private String normalisee;

    /**
     * Creates a platform's command analyser which splits the command into separated Strings.
     * 
     * @param cmd the platform's command to split
     * @throws platform.CommandSyntaxException Exception raised if the command is syntaxically incorrect.
     */
    public CommandAnalyser(String cmd) throws CommandSyntaxException {
        commande = null;
        listeEntrees = null;
        listeSorties=null;
        normalisee = null;
        normaliseLigne(cmd); // decoupage de la ligne de commande en parties
    }
    
    /**
     * Returns the splitted command. Each word of the command is in a String.
     * The input list for a component is a string on the form: [input1 input2 ....]
     * The output list for a component is a string on the form: [output1 output2 ....]
     * @return the splitted command.
     */
    public String[] getSplittedCommand() { return commande; }

    /**
     * Returns the splitted input list for a component. Each input is a string. 
     * @return the splitted input list for a component.
     */
    public String[] getSplittedEntryList() { return listeEntrees; }

    /**
     * Returns the splitted output list for a component. Each output is a string.
     * @return the splitted output list for a component.
     */
    public String[] getSplittedOutputList() { return listeSorties; }

    /**
     * Returns the command in a normalised form. Each part of the command is separated by only one space.
     * @return the command in a normalised form.
     */
    public String getNormalisedCommand() { return normalisee; }

    private void normaliseLigne(String cmd) throws CommandSyntaxException {
        char[] debut = {debutListe};
        char[] fin = {finListe};
        decoupeCommande(cmd);
        if (commande == null) {
            throw new CommandSyntaxException();
        }
        if (commande.length != 0) {
            normalisee = commande[0];
            for (int i = 1; i < commande.length; i++) {
                if (commande[i].startsWith(new String(debut))) {
                    if (listeEntrees == null) {
                        decoupeListe(commande[i]);
                        if (listeEntrees == null) {
                            throw new CommandSyntaxException();
                        }
                        if (listeEntrees.length != 0) {
                            normalisee = normalisee.concat(" " + new String(debut) + listeEntrees[0]);
                            for (int j = 1; j < listeEntrees.length; j++) {
                                normalisee = normalisee.concat(" " + listeEntrees[j]);
                            }
                            normalisee = normalisee.concat(new String(fin));
                        }
                        else {
                            throw new CommandSyntaxException();
                        }
                    }
                    else {
                        decoupeListe(commande[i]);
                        if (listeSorties == null) {
                            throw new CommandSyntaxException();
                        }
                        if (listeSorties.length != 0) {
                            normalisee = normalisee.concat(" " + new String(debut) + listeSorties[0]);
                            for (int j = 1; j < listeSorties.length; j++) {
                                normalisee = normalisee.concat(" " + listeSorties[j]);
                            }
                            normalisee = normalisee.concat(new String(fin));
                        }
                        else {
                            throw new CommandSyntaxException();
                        }
                    }
                }
                else {
                    normalisee = normalisee.concat(" " + commande[i]);
                }
            }
        }
        else {
            throw new CommandSyntaxException();
        }
    }

    private int parser(char c) {
        if ((c == separateur1) || (c == separateur2)) {
            return SEPARATEUR;
        }
        if (c == debutListe) {
            return DEBUT_LISTE;
        }
        if (c == finListe) {
            return FIN_LISTE;
        }
        return STANDARD_CHAR;
    }

    private void decoupeCommande(String cmd) {
        Vector<String> decoupage = new Vector<String>();
        int etat = 1;
        String courant = new String("");
        char[] caractere = new char[1];
        int i = 0;
        while (i < cmd.length()) {
            char c = cmd.charAt(i);
            switch (etat) {
                case 1:
                    if (parser(c) != SEPARATEUR) {
                        etat = 2;
                        courant = new String("");
                    }
                    else {
                        i++;
                    }
                    break;
                case 2:
                    if (parser(c) == SEPARATEUR) {
                        etat = 1;
                        if (courant.length() != 0) {
                            decoupage.addElement(courant);
                        }
                        courant = new String("");
                    }
                    else if (parser(c) == DEBUT_LISTE) {
                        etat = 3;
                        if (courant.length() != 0) {
                            decoupage.addElement(courant);
                        }
                        courant = new String("");
                    }
                    else {
                        caractere[0] = c;
                        courant = courant.concat(new String(caractere));
                        i++;
                        if ((i == cmd.length()) && (courant.length() != 0)) {
                            decoupage.addElement(courant);
                        }
                    }
                    break;
                case 3:
                    if (parser(c) == FIN_LISTE) {
                        etat = 1;
                        caractere[0] = c;
                        courant = courant.concat(new String(caractere));
                        decoupage.addElement(courant);
                        courant = new String("");
                        i++;
                    }
                    else {
                        caractere[0] = c;
                        courant = courant.concat(new String(caractere));
                        i++;
                        if ((i == cmd.length()) && (courant.length() != 0)) {
                            decoupage.addElement(courant);
                        }
                    }
                    break;
            }
        }
        if (etat == 3) {
            commande = null;
        }
        else {
            commande = new String[decoupage.size()];
            for (int j = 0; j < decoupage.size(); j++) {
                commande[j] = decoupage.elementAt(j);
            }
        }
    }

    private void decoupeListe(String liste) {
        Vector<String> decoupage = new Vector<String>();
        int etat = 1;
        String courant = "";
        char[] caractere = new char[1];
        int i = 0;
        boolean fini = false;
        while ((i < liste.length()) && (!fini)) {
            char c = liste.charAt(i);
            switch (etat) {
                case 1:
                    if (parser(c) == DEBUT_LISTE) {
                        etat = 2;
                        courant = new String("");
                        i++;
                    }
                    else {
                        i++;
                    }
                    break;
                case 2:
                    if (parser(c) != SEPARATEUR) {
                        etat = 3;
                    }
                    else {
                        i++;
                        courant = new String("");
                    }
                    break;
                case 3:
                    if (parser(c) == SEPARATEUR) {
                        if (courant.length() != 0) {
                            decoupage.addElement(courant);
                        }
                        etat = 2;
                    }
                    else if (parser(c) == FIN_LISTE) {
                        etat = 4;
                        if (courant.length() != 0) {
                            decoupage.addElement(courant);
                        }
                        fini = true;
                    }
                    else {
                        caractere[0] = c;
                        courant = courant.concat(new String(caractere));
                        i++;
                    }
                    break;
            }
        }
        if (etat != 4) {
            listeEntrees = null;
        } 
        else {
            if (listeEntrees == null) {
                listeEntrees = new String[decoupage.size()];
                for (int j = 0; j < decoupage.size(); j++) {
                    listeEntrees[j] = decoupage.elementAt(j);
                }
            }
            else {
                listeSorties = new String[decoupage.size()];
                for (int j = 0; j < decoupage.size(); j++) {
                    listeSorties[j] = decoupage.elementAt(j);
                }
            }
        }
    }
}
