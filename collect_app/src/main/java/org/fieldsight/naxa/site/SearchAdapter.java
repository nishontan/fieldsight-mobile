package org.fieldsight.naxa.site;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.bcss.collect.android.R;;
import org.fieldsight.naxa.common.ViewUtils;
import org.fieldsight.naxa.login.model.Site;

import java.util.List;

public class SearchAdapter extends BaseAdapter {

    private final Context mContext;
    private List<Site> siteList;
    private LayoutInflater mLayoutInflater;

    public SearchAdapter(Context context, List<Site> siteList) {
        this.mContext = context;
        this.siteList = siteList;

    }


    public void updateList(List<Site> filterList, boolean isFilterList) {
        this.siteList = filterList;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return siteList != null ? siteList.size() : 0;
    }

    @Override
    public String getItem(int position) {
        return siteList.get(position).getName();
    }

    public Site getMySiteLocationPojo(int position) {
        return siteList.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = null;
        if (v == null) {

            holder = new ViewHolder();

            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            v = mLayoutInflater.inflate(R.layout.list_item_search, parent, false);
            holder.tvSiteName = v.findViewById(R.id.search_item_site_name);
            holder.tvSiteId = v.findViewById(R.id.search_item_site_identifier);
            holder.tvSiteAddress = v.findViewById(R.id.search_item_site_address);
            holder.tvPhoneNumber = v.findViewById(R.id.search_item_site_phone_number);
            holder.tvIconText = v.findViewById(R.id.title_desc_tv_icon_text);

            v.setTag(holder);
        } else {

            holder = (ViewHolder) v.getTag();
        }

        Site site = siteList.get(position);
        ViewUtils.showOrHide(holder.tvIconText, site.getName().substring(0, 1));
        ViewUtils.showOrHide(holder.tvSiteName, site.getName());
        ViewUtils.showOrHide(holder.tvSiteId, site.getIdentifier());
        ViewUtils.showOrHide(holder.tvPhoneNumber, site.getPhone());
        ViewUtils.showOrHide(holder.tvSiteAddress, site.getAddress());


        return v;
    }


}

class ViewHolder {
    TextView tvSiteName, tvIconText, tvSiteId, tvSiteAddress, tvPhoneNumber;

}





