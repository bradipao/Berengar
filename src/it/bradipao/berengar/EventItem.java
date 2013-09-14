package it.bradipao.berengar;

public class EventItem {

   public long l_id,l_idcat,l_when;
   public String s_name;

   public EventItem(long l_idcat,long l_when,String s_name) {
      super();
      this.l_id = 0;
      this.l_idcat = l_idcat;
      this.l_when = l_when;
      this.s_name = s_name;
   }
   
   public EventItem(long l_id,long l_idcat,long l_when,String s_name) {
      super();
      this.l_id = l_id;
      this.l_idcat = l_idcat;
      this.l_when = l_when;
      this.s_name = s_name;
   }
   
}
