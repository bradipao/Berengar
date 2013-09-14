package it.bradipao.berengar;

public class CategoryItem {

   public long l_id;
   public String s_name,s_icon;
   public int i_icon;

   public CategoryItem(String s_name,String s_icon) {
      super();
      this.l_id = 0;
      this.s_name = s_name;
      this.s_icon = s_icon;
      this.i_icon = 0;
   }
   
   public CategoryItem(String s_name,String s_icon,int i_icon) {
      super();
      this.l_id = 0;
      this.s_name = s_name;
      this.s_icon = s_icon;
      this.i_icon = i_icon;
   }
   
   public CategoryItem(long l_id,String s_name,String s_icon) {
      super();
      this.l_id = l_id;
      this.s_name = s_name;
      this.s_icon = s_icon;
      this.i_icon = 0;
   }
   
}
