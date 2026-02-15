package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Errors.Message;
import ssw.mj.scanner.Token;
import ssw.mj.scanner.Token.Kind;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class Scanner {

  // Scanner Skeleton - do not rename fields / methods !
  private static final char EOF = (char) -1;
  private static final char CR = '\r';
  private static final char LF = '\n';

  /**
   * Input data to read from.
   */
  private final Reader in;

  /**
   * Lookahead character. (= next (unhandled) character in the input stream)
   */
  private char ch;

  /**
   * Current line in input stream.
   */
  private int line;

  /**
   * Current column in input stream.
   */
  private int col;

  /**
   * According errors object.
   */
  public final Errors errors;

  public Scanner(Reader r) {
    // store reader
    in = r;

    // initialize error handling support
    errors = new Errors();

    line = 1;
    col = 0;
    nextCh(); // read 1st char into ch, increment col to 1
  }

  /**
   * Adds error message to the list of errors.
   */
  public final void error(Token t, Message msg, Object... msgParams) {
    errors.error(t.line, t.col, msg, msgParams);

    // reset token content (consistent JUnit tests)
    t.numVal = 0;
    t.val = null;
  }


  // ================================================
  // TODO Exercise UE-P-1: Implement Scanner (next() + private helper methods)
  // ================================================

  // TODO Exercise UE-P-1: Keywords
  /**
   * Mapping from keyword names to appropriate token codes.
   */
  private static final Map<String, Kind> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put(Kind.break_.label(), Kind.break_);
    keywords.put(Kind.class_.label(), Kind.class_);
    keywords.put(Kind.else_.label(), Kind.else_);
    keywords.put(Kind.final_.label(), Kind.final_);
    keywords.put(Kind.if_.label(), Kind.if_);
    keywords.put(Kind.new_.label(), Kind.new_);
    keywords.put(Kind.print.label(), Kind.print);
    keywords.put(Kind.program.label(), Kind.program);
    keywords.put(Kind.read.label(), Kind.read);
    keywords.put(Kind.return_.label(), Kind.return_);
    keywords.put(Kind.void_.label(), Kind.void_);
    keywords.put(Kind.while_.label(), Kind.while_);
  }

  /**
   * Returns next token. To be used by parser.
   */
  public Token next() {
    // TODO Exercise UE-P-1: implementation of next method

    while (Character.isWhitespace(ch)) {
      nextCh();
    }

    Token t = new Token(Kind.none, line, col);

    if (isLetter(ch)) {
      readName(t);
    } else if (isDigit(ch)) {
      readNumber(t);
    } else {
      switch (ch) {
        case '\'':
          nextCh();
          readCharConst(t);
          break;
        // --- cumulative tokens ---
        case '+':
          nextCh();
          if (ch == '=') {
            setToken(t, Kind.plusas);
            nextCh();
          } else if (ch == '+') {
            setToken(t, Kind.pplus);
            nextCh();
          } else {
            setToken(t, Kind.plus);
            // no nextCh()
          }
          break;
        case '-':
          nextCh();
          if (ch == '=') {
            setToken(t, Kind.minusas);
            nextCh();
          } else if (ch == '-') {
            setToken(t, Kind.mminus);
            nextCh();
          } else {
            setToken(t, Kind.minus);
            // no nextCh()
          }
          break;
        case '*':
          nextCh();
          if (ch == '=') {
            setToken(t, Kind.timesas);
            nextCh();
          } else {
            setToken(t, Kind.times);
            // no nextCh()
          }
          break;
        case '/':
          nextCh();
          if (ch == '*') {
            nextCh();
            skipComment(t);
            t = next();
          } else if (ch == '=') {
            setToken(t, Kind.slashas);
            nextCh();
          } else {
            setToken(t, Kind.slash);
            // no nextCh()
          }
          break;
        case '%':
          nextCh();
          if (ch == '=') {
            setToken(t, Kind.remas);
            nextCh();
          } else {
            setToken(t, Kind.rem);
            // no nextCh()
          }
          break;
        case '=':
          nextCh();
          if (ch == '=') {
            setToken(t, Kind.eql);
            nextCh();
          } else {
            setToken(t, Kind.assign);
            // no nextCh()
          }
          break;
        case '!':
          nextCh();
          if (ch == '='){
            setToken(t, Kind.neq);
            nextCh();
          } else {
            error(t, Message.INVALID_CHAR, '!');
            // no nextCh()
          }
          break;
        case '<':
          nextCh();
          if (ch == '=') {
            setToken(t, Kind.leq);
            nextCh();
          } else {
            setToken(t, Kind.lss);
            // no nextCh()
          }
          break;
        case '>':
          nextCh();
          if (ch == '=') {
            setToken(t, Kind.geq);
            nextCh();
          } else {
            setToken(t, Kind.gtr);
            // no nextCh()
          }
          break;
        case '&':
          nextCh();
          if (ch == '&') {
            setToken(t, Kind.and);
            nextCh();
          } else {
            error(t, Message.INVALID_CHAR, '&');
            // no nextCh()
          }
          break;
        case '|':
          nextCh();
          if (ch == '|') {
            setToken(t, Kind.or);
            nextCh();
          } else {
            error(t, Message.INVALID_CHAR, '|');
            // no nextCh()
          }
          break;
        // --- simple tokens ---
        case ';':
          setToken(t, Kind.semicolon);
          nextCh();
          break;
        case ',':
          setToken(t, Kind.comma);
          nextCh();
          break;
        case '.':
          setToken(t, Kind.period);
          nextCh();
          break;
        case '(':
          setToken(t, Kind.lpar);
          nextCh();
          break;
        case ')':
          setToken(t, Kind.rpar);
          nextCh();
          break;
        case '[':
          setToken(t, Kind.lbrack);
          nextCh();
          break;
        case ']':
          setToken(t, Kind.rbrack);
          nextCh();
          break;
        case '{':
          setToken(t, Kind.lbrace);
          nextCh();
          break;
        case '}':
          setToken(t, Kind.rbrace);
          nextCh();
          break;
        case '~':
          setToken(t, Kind.tilde);
          nextCh();
          break;
        case EOF:
          setToken(t, Kind.eof);
          // no nextCh() should be called after EOF
          break;
        default:
          error(t, Message.INVALID_CHAR, ch);
          nextCh();
          break;
      } // end switch
    } // end else

    return t;
  }

  private void nextCh() {
    // TODO Exercise UE-P-1: implementation of nextCh method and other private helper methods
    try {
      ch = (char) in.read();
      col++;
      if (ch == LF) {
        line++;
        col = 0;
        // TODO ? A test case might be missing. singleIdent rightfully fails with the following line statement uncommented, but nextChar
        //  might still have to be called in some different way after recognizing an LF. Example where my code might
        //  still fail: "+'\n'="
        // nextCh();
      } /*else if (ch == CR) {
        // TODO do something instead of ignoring?
      }*/
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO Exercise UE-P-1: private helper methods used by next(), as discussed in the exercise

  private void readName(Token t) {
    // ch should be a letter when this method is called
    StringBuilder stringBuilder = new StringBuilder();

    do {
      stringBuilder.append(ch);
      nextCh();
    } while (isLetter(ch) || isDigit(ch) || ch == '_');

    String ident = stringBuilder.toString();

    if (keywords.containsKey(ident)) {
      setToken(t, keywords.get(ident));
    } else { // is normal ident
      t.kind = Kind.ident;
      t.val = ident;
    }

    // next ch is already prepared
  }

  private void readNumber(Token t) {
    // ch should be a digit when this method is called
    t.kind = Kind.number;
    StringBuilder stringBuilder = new StringBuilder();

    do {
      stringBuilder.append(ch);
      nextCh();
    } while (isDigit(ch));

    String stringOfNumber = stringBuilder.toString();

    if (stringOfNumber.length() >= 19) { // probably does not even fit in long => really big number
      error(t, Message.BIG_NUM, stringOfNumber);
    } else {
      long number = Long.parseLong(stringOfNumber);
      if (number > Integer.MAX_VALUE) { // fits in long, but not in int
        error(t, Message.BIG_NUM, stringOfNumber);
      } else { // fits in int
        t.val = stringOfNumber;
        t.numVal = (int) number;
      }
    }

    // next ch is already prepared
  }

  private void readCharConst(Token t) {
    t.kind = Kind.charConst;

    // ch set to first char after start of charConst
    char firstCh = ch;
    switch (firstCh) {
      case '\'':
        error(t, Message.EMPTY_CHARCONST);
        t.val = "\0";
        t.numVal = '\0';
        nextCh();
        return;
      case EOF:
        error(t, Message.EOF_IN_CHAR);
        t.val = "\0";
        t.numVal = '\0';
        // no nextCh() should be called after EOF
        return;
      case CR:
        nextCh();
        if (ch == LF) {
          nextCh();
        }
        error(t, Message.ILLEGAL_LINE_END);
        t.val = "\0";
        t.numVal = '\0';
        return;
      case LF:
        error(t, Message.ILLEGAL_LINE_END);
        t.val = "\0";
        t.numVal = '\0';
        nextCh();
        return;
      default: // no error yet
        break;
    }

    nextCh();
    if (ch == EOF) {
      //error(t, Message.EOF_IN_CHAR, ch);
      error(t, Message.MISSING_QUOTE);
      t.val = "\0";
      t.numVal = '\0';
      // no nextCh() should be called after EOF
    } else if (firstCh == '\\') { // found escape sequence
      if (ch == '\\') {
        t.val = String.valueOf(ch);
        t.numVal = ch;
      } else if (ch == 'n') {
        t.val = String.valueOf('\n');
        t.numVal = '\n';
      } else if (ch == 'r') {
        t.val = String.valueOf('\r');
        t.numVal = '\r';
      } else if (ch == '\'') {
        t.val = String.valueOf('\'');
        t.numVal = '\'';
      } else {
        error(t, Message.UNDEFINED_ESCAPE, ch);
        t.val = "\0";
        t.numVal = '\0';
      }

      nextCh();
      if (ch == '\'') {
        nextCh();
      } else {
        error(t, Message.MISSING_QUOTE);
        t.val = "\0";
        t.numVal = '\0';
        // no nextCh()
      }
    } else if (ch == '\'') { // found regular charConst
      t.val = String.valueOf(firstCh);
      t.numVal = firstCh;
      nextCh();
    } else {
      error(t, Message.MISSING_QUOTE);
      t.val = "\0";
      t.numVal = '\0';
      // no nextCh()
    }
  }

  private void skipComment(Token t) {
    int nOpenComments = 1;

    while (nOpenComments > 0) {
      if (ch == '/') { // check for start of a comment
        nextCh();
        if (ch == '*') { // found start of a comment
          nOpenComments++;
          nextCh();
        }
      } else if (ch == '*') { // check for end of a comment
        nextCh();
        if (ch == '/') { // found end of a comment
          nOpenComments--;
          nextCh();
        }
      } else if (ch == EOF) {
        setToken(t, Kind.eof);
        error(t, Message.EOF_IN_COMMENT);
        // no nextCh() should be called after EOF
        return;
      } else { // neither start, nor end of a comment, still before EOF
        nextCh();
      }
    }
  }

  // -----------------------------------------------

  private static boolean isLetter(char c) {
    return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z';
  }

  private static boolean isDigit(char c) {
    return '0' <= c && c <= '9';
  }

  private static void setToken(Token token, Kind kind) {
    token.kind = kind;
    token.val = kind.label();
  }

  // ================================================
  // ================================================
}
