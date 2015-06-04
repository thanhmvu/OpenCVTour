package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Fragment to display the list of TourItems in a Tour.
 */
public class EditTourItemListFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

	private ListView _list_view;

	//private ArrayList<TourItem> _tour_items;
	private Tour _tour;

	private EditTourItemListFragment _listener = this;


	public void onItemClick(AdapterView parent, View v, int position, long id) {
		// Switch to TourItem-editing mode.
		Intent intent = new Intent(getActivity(), EditTourItemActivity.class);
		Bundle bundle = new Bundle();
		bundle.putShort("position", (short) position);

		intent.putExtras(bundle);
		startActivityForResult(intent, EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST);
	}



	public class TourItemArrayAdapter extends ArrayAdapter<TourItem>
	{
		private final Context _context;
		private final List<TourItem> _items;
		private final HashMap<String, Bitmap> _thumbnails = new HashMap<>();

		final int INVALID_ID = -1;

		public TourItemArrayAdapter(Context context, List<TourItem> items)
		{
			super(context, -1, items);
			_context = context;
			_items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			TourItem item = _items.get(position);

			/// Create row_view, or recycle an existing view if possible
			View row_view;
			if(convertView == null) {
				LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row_view = inflater.inflate(R.layout.tour_item_line, parent, false);
			} else {
				row_view = convertView;
			}

			((TextView) row_view.findViewById(R.id.tour_item_name)).setText(item.getName());
			((TextView) row_view.findViewById(R.id.tour_item_description)).setText(item.getDescription());

			if(item.hasMainImage()) {
				if(!_thumbnails.containsKey(item.getMainImageFilename()))
					_thumbnails.put(item.getMainImageFilename(), Utilities.decodeSampledBitmap(item.getMainImageFilename(), 64, 64));
				((ImageView) row_view.findViewById(R.id.tour_item_thumbnail)).setImageBitmap(_thumbnails.get(item.getMainImageFilename()));
			} else ((ImageView) row_view.findViewById(R.id.tour_item_thumbnail)).setImageResource(R.drawable.default_thumbnail);

			/// Todo: audio support

			/// Set up event listeners for the item's buttons
			row_view.findViewById(R.id.delete_tour_item).setOnClickListener(_listener);

			return row_view;
		}

		@Override
		public long getItemId(int position) {
			if (position < 0 || position >= _items.size()) {
				return INVALID_ID;
			}
			return getItem(position).getId();
		}

		public List<TourItem> getList() {
			return _items;
		}

	}




	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_edit_tour_item_list, container, false);

		v.findViewById(R.id.add_tour_item).setOnClickListener(this);

		return v;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_tour = Tour.getCurrentTour();

		_list_view = (ListView) getActivity().findViewById(R.id.list);

		ArrayList<TourItem> _tour_items = _tour.getTourItems();

		/// Set up _list_view to show the items in our list.
		TourItemArrayAdapter adapter = new TourItemArrayAdapter(getActivity(), _tour_items);
		_list_view.setAdapter(adapter);

		_list_view.setOnItemClickListener(this);
	}


	@Override
	public void onClick(View v)
	{
		switch(v.getId()) {
			case R.id.add_tour_item:
				addNewTourItem();
				break;
			case R.id.delete_tour_item: {
				final int position = _list_view.getPositionForView(v);
				if (position != ListView.INVALID_POSITION) {
					/// Delete that TourItem
					((TourItemArrayAdapter) _list_view.getAdapter()).remove(((TourItemArrayAdapter) _list_view.getAdapter()).getItem(position));
				}
			}
		}
	}

	/**
	 * Adds a new TourItem, and launches EditTourItemActivity for it.
	 */
	public void addNewTourItem()
	{
		/// Add empty TourItem to the Tour
		_tour.getTourItems().add(new TourItem());
		((TourItemArrayAdapter) _list_view.getAdapter()).notifyDataSetChanged();

		/// Switch to TourItem-editing mode.
		Intent intent = new Intent(getActivity(), EditTourItemActivity.class);
		Bundle bundle = new Bundle();
		bundle.putShort("position", (short) (_tour.getTourItems().size() - 1));
		intent.putExtras(bundle);
		startActivityForResult(intent, EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		/// Update our ListView if the EditTourItemActivity finished successfully.
		if(resultCode == Activity.RESULT_OK && requestCode == EditTourItemActivity.EDIT_TOUR_ITEM_REQUEST) {
			((TourItemArrayAdapter) _list_view.getAdapter()).notifyDataSetChanged();
		}
	}


}